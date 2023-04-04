FROM python:3.11.2-slim

ENV PYTHONUNBUFFERED True

COPY src /pushproxy
WORKDIR /pushproxy

RUN pip install --require-hashes -r requirements.txt

CMD exec gunicorn \
    --bind 0.0.0.0:8080 \
    --workers 4 \
    --threads 8 \
    --timeout 0 \
    -e FORWARD_JWT=$FORWARD_JWT \
    -e NAMESERVERS=$NAMESERVERS \
    proxy:app
