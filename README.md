# CF to IKS

## Prerequisites

- Install the [IBMCloud CLI](https://cloud.ibm.com/docs/cli?topic=cli-getting-started)
- Install the [IBMCloud Container Service Plugin](https://cloud.ibm.com/docs/containers?topic=containers-cs_cli_install)
- Install the [IBMCloud Code Engine Plugin](https://cloud.ibm.com/docs/codeengine?topic=codeengine-cli)
- [Docker](https://docs.docker.com/engine/install/)
- Install the [IBMCloud Container Registry Plugin](https://cloud.ibm.com/docs/Registry?topic=container-registry-cli-plugin-containerregcli) (or have your own container registry)
- Optional: Manage Kubernetes easier with [k9s](https://k9scli.io/topics/install/)

### Creating a Dockerfile

The first step to be able to deploy your application on Kubernetes is to containerize it. There is an example [Dockerfile](/Dockerfile) in this repository for a springboot application. This Dockerfile is a two stage dockerfile, which consists of building the jar, then packaging it (moving it into the container).

After creating your Dockerfile, build the docker image:

```bash
docker build -t <name> .
```

If the image succesfully builds, try running the image:

```bash
docker run -it -p 8080:8080 <name>:latest
```

A couple of notes here on the flags:

- `it` is a shorthand for `-i` and `-t`, commonly used together. `-i` allows input to be passed to the container as it runs, and `-t` creates a pseudo-terminal that the container can attach to. When used together, it's often referred to as "interactive terminal"
- `p` is the publish flag, which means you publish from the exposed port to the port you want on the container. This essentially forwards the container port to your computer's port, so you can see the container's output, if it's a web app.

If the container is running as expected, you can continue to the next step.

### Pushing an image to the IBM Container Registry

First, login to the IBM Cloud Container Registry. You must have the IBM Cloud CLI installed as well as docker.

```bash
ibmcloud login
ibmcloud cr login
```

Once you are logged in, you will need to tag your image to your ibm cloud container registry namespace like this:

```bash
docker tag <local image name> us.icr.io/<namespace>/<image name>
```

Once the iamge is tagged, you can push the image:

```bash
docker push us.icr.io/<namespace>/<image name>
```

Check that your image has pushed successfully:

```bash
ibmcloud cr images --restrict <namespace>
```

### Accessing your image in your Kubernetes Deployment

Before continuing, ensure that your Kubernetes Cluster contains the secret `all-icr-io`, if you are using the IBMCloud Container Registry. If you are using a different container registry, ensure you have created a Kubernetes Secret to be able to access that registry. If you have neither of these, follow the instructions [here](https://cloud.ibm.com/docs/Registry?topic=containers-ts-app-image-pull) to ensure you will be able to pull your image down when you create a deployment. This will likely be a (easily solved) problem if you are using a Kubernetes Namespace.

### Deploying your application

By now, the Kubernetes Cluster may be done provisioning. Switch your kubernetes context to that cluster.

```
ibmcloud ks cluster config --cluster <cluster>
```

This allows you to use `kubectl` commands with your IKS Cluster.

If you are using a namespace, you can update your `kubectl` context to include the namespace in all future commands:

```
kubectl config set-context --current --namespace isaac-namespace
```

Ensure your context is set:

```
kubectl config current-context
```

Use the [deployment.yml](deployment.yml) file to create a deployment for your cluster. In this file, you will see multiple fields marked "change me" - adjust these fields to the correct values for your app, like your app name.

Note that you must change your image pull secret, or add another one if you are not using the IBM Cloud Container Registry.

Once you have finished editing your deployment file, you can apply the deployment file as such:

```
kubectl apply -f deployment.yml
```

For all of the commands below, if you are not using a namespace, you do not need to include the `--namespace` flag.

```
kubectl get deployments --namespace default
```

### Creating a service

To publicly expose your deployment, on a VPC infrastructure, you will want to create a load balancer. You can create this with a yaml file as done before, like the one in `service.yml`, or, for simplicity, you can use this command:

```bash
kubectl expose deployment/<deployment name> --type=LoadBalancer --name=<name>-lb-svc  --port=8080 --target-port=8080 -n <namespace>
```

If you wish to use a yaml file, it is the same command as before:

```bash
kubectl apply -f service.yml
```

Once you have created your service, get the details about it. You will see an external IP either provisioning or available for your application.

```bash
kubectl describe service <name> -n <namespace>
```

If your service is not immediately available, you can check the status of your load balancer. To do this, make sure you have the IBM Cloud Infrastructure Service plugin installed (`ibmcloud plugin install infrastructure-service`)

```bash
ibmcloud is load-balancers
```

This will show a list of load balancers if you have multiple in the format `kube-<cluster id>-<load balancer id>`. You will want to find the one that has a load balancer id that is the same as your load balancer, and check if it is public, on, and active.

To find the load balancer id of your service, use this command:

```bash
kubectl get svc <service name> -o yaml -n <namespace>
```

This will output, as yaml, the yaml used to configure your service. Look for `metadata.uid` and you will find the load balancer's id.

Once the service is active, you will be able to access it publicly via `<external ip>:<port>`

### Binding a service, like a postgres db:

Look at this documentation [here](https://cloud.ibm.com/docs/containers?topic=containers-service-binding) for more information on service bindings.
