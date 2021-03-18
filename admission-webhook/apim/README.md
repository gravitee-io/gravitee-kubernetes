# Generate CA

Here is a way to generate a certificate authority and a certificate for the gateway.

NOTE: The common name used in the server certificate must match the full qualified name of the service used to expose the webhook endpoint. 

```
# Create certificate authority.
openssl req -newkey rsa:4096 -keyform PEM -keyout ca.key -x509 -days 3650 -subj "/CN=gateway-admission-ca" -passout pass:ca-secret -outform PEM -out ca.pem
openssl pkcs12 -export -inkey ca.key -in ca.pem -out ca.p12 -passin pass:ca-secret -passout pass:ca-secret -name adminssion-webhook-ca

# Server key (localhost)
openssl genrsa -out server.key 4096
openssl req -new -key server.key -out server.csr -sha256 -subj "/CN=gateway-admission-webhook.default.svc"
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -set_serial 100 -extensions server -days 1460 -outform PEM -out server.cer -sha256 -passin pass:ca-secret
openssl pkcs12 -export -inkey server.key -in server.cer -out server.p12 -passout pass:server-secret -name gateway-server

```

## generate CABundle value

ValidatingWebhookConfiguration needs the CA in PEM format encoded in base64, here is how to obtain the value to set in the caBundle entry.

```
$( cat ca.pem | base64 -w 0 )
```

