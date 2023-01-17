# Code Engine to IKS

Note: This documentation is written for a Springboot Application connected to a PostgreSQL database.

## Prerequisites

- Install the [IBMCloud CLI](https://cloud.ibm.com/docs/cli?topic=cli-getting-started)
- Install the [IBMCloud Container Service Plugin](https://cloud.ibm.com/docs/containers?topic=containers-cs_cli_install)
- Install the [IBMCloud Code Engine Plugin](https://cloud.ibm.com/docs/codeengine?topic=codeengine-cli)
- [Docker](https://docs.docker.com/engine/install/)
- Install the [IBMCloud Container Registry Plugin](https://cloud.ibm.com/docs/Registry?topic=container-registry-cli-plugin-containerregcli) (or have your own container registry)
- Optional: Manage Kubernetes easier with [k9s](https://k9scli.io/topics/install/)

## Migration Steps

Before getting started, you must provision a cluster of the IBMCloud Kubernetes service.

You will want to create a project directory for this project, as a couple of files will be generated and written:

```
mkdir ce-to-iks && cd ce-to-iks
```

### Gathering secrets from your Code Engine Project

While our cluster is provisioning, we will gather the information we need from our Code Engine project to migrate it to IKS. To start, log into the IBMCloud CLI. If you are using a federated ID, add the `--sso` tag at the end of the command:

```
ibmcloud login
```

After you are logged in, target the resource group your code engine project is in. For this example, I will target the resource group "playground"

```
ibmcloud target -g playground
```

What we need from the project is the `ce-services` secret. We will use this directly in our IKS deployment. To get this secret, you must access the kubecfg of your code engine project. Select your code engine project, and add the flag --kubecfg to add this to your current kubernetes context.

```
ibmcloud ce project select --name <project name> --kubecfg
```

Now, you are able to use `kubectl` commands to access your Code Engine project. Next, list all the secrets in your CE Project:

```
kubectl get secrets
```

In this list, you will see a secret named `ce-services`. This is the secret will need to be able to use all the services we had attached to our Code Engine project when we redeploy the image on IKS. To save this configuration to your local filesystem, do the following command:

```
kubectl get secret ce-services -o yaml > ce_services.yml
```

When you open this file, you should see something similar to what is below. There may be a few entries in here, as this contains the secrets for every application on your code engine project. Feel free to remove any unncessary secrets, and only keep the secret for the application you are migrating.

### Creating your image and adding it to the container registry

To deploy on IKS, you must have a container image for your application stored in some registry. For the purposes of this tutorial, this will be done in the IBM Cloud Container Registry.

First, you must have a Dockerfile for your application. If you are migrating a Springboot application, the [Dockerfile](Dockerfile) in this repository will be a great starting point. From there, you can build your image:

```
docker build -t <image name> .
```

Next, you will want to log into your container registry:

```
ibmcloud cr login
```

And additionally create a container registry namespace, if you don't have one:

```
ibmcloud cr namespace-add <namespace name>
```

Tag your docker image so that docker knows where to send your image when you push it:

```
docker tag <image name>:<version> us.icr.io/<namespace name>/<image name>:<version>
```

The versioning is optional, if left out, it will be populated to `latest`.

Now that the image is tagged, you can push your image to the Container Registry:

```
docker push
```

Check that your image pushed successfully via:

```
ibmcloud cr images --restrict <namespace>
```

### Accessing your image in your Kubernetes Deployment

Before continuing, ensure that your Kubernetes Cluster contains the secret `all-icr-io`, if you are using the IBMCloud Container Registry. If you are using a different container registry, ensure you have created a Kubernetes Secret to be able to access that registry. If you have neither of these, follow the instructions [here](https://cloud.ibm.com/docs/Registry?topic=containers-ts-app-image-pull) to ensure you will be able to pull your image down when you create a deployment. This will likely be a (easily solved) problem if you are using a Kubernetes Namespace.

### Deploying your application and creating a service

By now, the Kubernetes Cluster may be done provisioning. Switch your kubernetes context to that cluster. Note: If your code engine project and kubernetes cluster are in different resource groups, you will have to change your resource group to be able to change your cluster context.

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

Note that you must change your image pull secret, or add another one if you are not using the IBM Cloud Container Registry. Additionally, if you are using a secret other than ce-services, you will want to add those under `env`.

Once you have finished editing your deployment file, you can apply the deployment file as such:

```
kubectl apply -f deployment.yml
```

For all of the commands below, if you are not using a namespace, you do not need to include the `--namespace` flag.

```
kubectl get deployments --namespace default
```

Now you can check your pods:

```
kubectl get pods --namespace default
```

You should see your containers either running in your pods or with the `ContainerCreating` status.

Now, create a service. Open the [service.yml](service.yml) file to edit the configuration. There are a few fields in here that will need to be edited in order for the configuration to work properly. These are marked with "change me."

Services are created in Kubernetes to allow your pods to be accessed, either internally to the cluster or externally. Since we are deploying an application we want to be exposed, we are saying the `type` is a LoadBalancer, which will supply us with an External IP which we will use later for connecting. When you are complete, apply the configuration of the service.

```
kubectl apply -f service.yml
```

Check the service:

```
kubectl get services --namespace default
```

If you need more detail, or want to see the endpoints, you can always describe the service:

```
kubectl describe service <service name> --namespace default
```

Now, you should see the external IP. Use this to access your cluster, with the port you put in your services file: \<external ip>:\<port>
