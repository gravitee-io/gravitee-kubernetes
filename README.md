# Gravitee.io Kubernetes

This project provides a kubernetes controller for the APIM Gateway.

## Controller

The Controller is provided as a [plugin](gravitee-gateway-services-kubernetes).

This plugin isn't included into the Gateway bundle by default.

### Custom Resource Definitions

Kubernetes CRD used by the controller are available [here](crds/apim)

### Admission webhook

To enforce controls on custom resource, an admission webhook is provided by the controller plugin.

The webhook endpoint will be registered on the `core\http` gateway service (by default localhost:18082).

An example of `ValidatingWebhookConfiguration` definition is available [here](admission-webhook/apim)  

