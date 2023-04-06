import logging
import os
import sys

import dns.resolver
import requests

from http.client import HTTPConnection

from flask import Flask, request

app = Flask(__name__)

DEFAULT_NAMESERVERS = ["8.8.8.8", "4.4.4.4"]
FORWARD_JWT = os.environ.get("FORWARD_JWT").lower() == "true"


def _setup_logging():
    if os.environ.get("DEBUG"):
        logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
        requests_log = logging.getLogger("requests.packages.urllib3")
        requests_log.setLevel(logging.DEBUG)
        requests_log.propagate = True
        HTTPConnection.debuglevel = 1
    else:
        logging.basicConfig(stream=sys.stdout, level=logging.INFO)


def _setup_dns():
    nameservers = os.environ.get("NAMESERVERS").split(",")
    if len(nameservers) > 0:
        nameservers.extend(DEFAULT_NAMESERVERS)
        dns_resolver = dns.resolver.Resolver()
        dns_resolver.nameservers = nameservers
        dns.resolver.override_system_resolver(dns_resolver)


def _get_auth_header(request, target_host):
    if FORWARD_JWT:
        return request.headers.get("Authorization")

    response = requests.get(
        f"http://169.254.169.254/computeMetadata/v1/instance/service-accounts/default/identity?audience={target_host}",
        headers={"Metadata-Flavor": "Google"},
    )
    return f"Bearer {response.text}"


@app.route("/", methods=["POST"])
def index():
    envelope = request.get_json()
    if not envelope:
        msg = "no Pub/Sub message received"
        logging.error(msg)
        return f"Bad Request: {msg}", 400

    if not isinstance(envelope, dict) or "message" not in envelope:
        msg = "invalid Pub/Sub message format"
        logging.error(msg)
        return f"Bad Request: {msg}", 400

    logging.debug(f"Received event {envelope}")
    host = request.args.get("host")
    enduser_endpoint = (
        f"https://{host}{request.args.get('path')}?token={request.args.get('token')}"
    )
    logging.debug(f"Forwarding to host {enduser_endpoint}")

    session = requests.Session()

    headers = {"Authorization": _get_auth_header(request, host)}
    response = session.post(
        enduser_endpoint,
        json=request.get_json(),
        headers=headers,
        verify="/etc/ssl/certs/ca-certificates.crt",
    )

    return ("", response.status_code)


_setup_logging()
_setup_dns()
