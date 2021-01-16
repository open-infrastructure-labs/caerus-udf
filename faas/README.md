# Caerus Serverless (FaaS: Function-as-a-Service) Framework and Function Support
Caerus UDF supports FaaS build, deploy, and envocation. It currently uses Openfaas Serverless framework:
https://www.openfaas.com/

# Getting Started - Setup Openfaas Framework
Note: Fowllowing steps are to setup kubenetes cluster of openfaas, where step 1-7 are pre-requirements, and step 8-12 are steps for the framework deployment  
1. Install make :
```
> apt update
> apt install make
``` 
2. Install kubectl: https://kubernetes.io/docs/tasks/tools/install-kubectl/
```
> curl -LO "https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl"
> chmod +x ./kubectl
> sudo mv ./kubectl /usr/local/bin/kubectl
> kubectl version –client
```
3. Install golang:  https://golang.org/doc/install
```
> wget https://golang.org/dl/go1.15.6.linux-amd64.tar.gz
> tar -C /usr/local -xzf go1.15.6.linux-amd64.tar.gz
> export PATH=$PATH:/usr/local/go/bin
> go version
``` 
4. Install kind: https://kind.sigs.k8s.io/docs/user/quick-start/
```
> GO111MODULE="on" go get sigs.k8s.io/kind@v0.9.0
> kind
> curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.9.0/kind-linux-amd64
> chmod +x ./kind
> mv ./kind /usr/local/bin/kind
> kind
``` 
5.	Install docker: https://docs.docker.com/engine/install/ubuntu/
```
> sudo apt-get remove docker docker-engine docker.io containerd runc
> sudo apt-get update
> sudo apt-get install     apt-transport-https     ca-certificates     curl     gnupg-agent     software-properties-common
> curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
> sudo add-apt-repository    "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
> $(lsb_release -cs) \
  stable"
> sudo apt-get install docker-ce docker-ce-cli containerd.io
> sudo docker run hello-world
```
6.	Install helm: https://helm.sh/docs/intro/install/
```
> curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3
> chmod 700 get_helm.sh
> ./get_helm.sh
```
7.	Install faas-cli: https://github.com/openfaas/faas-cli
```
> curl -sSL https://cli.openfaas.com | sudo sh
```
8. Clone faas-netes: https://github.com/openfaas/faas-netes
```
> git clone https://github.com/openfaas/faas-netes.git
> cd faas-netes/
> make start-kind 
```
Output:
```
root@caerus-demo:~/openfaas/faas-netes# make start-kind
>>> Creating Kubernetes v1.18.8 cluster kind
Creating cluster "kind" ...
DEBUG: docker/images.go:58] Image: kindest/node:v1.18.8 present locally
✓ Ensuring node image (kindest/node:v1.18.8) 🖼
✓ Preparing nodes 📦
✓ Writing configuration 📜
✓ Starting control-plane 🕹️
✓ Installing CNI 🔌
✓ Installing StorageClass 💾
✓ Waiting ≤ 5m0s for control-plane = Ready ⏳
• Ready after 28s 💚
Set kubectl context to "kind-kind"
You can now use your cluster with:

kubectl cluster-info --context kind-kind

Have a nice day! 👋
>>> Waiting for CoreDNS
Waiting for deployment "coredns" rollout to finish: 0 of 2 updated replicas are available...
Waiting for deployment "coredns" rollout to finish: 1 of 2 updated replicas are available...
deployment "coredns" successfully rolled out
Applying namespaces
namespace/openfaas created
namespace/openfaas-fn created
secret/basic-auth created
Waiting for helm install to complete.
Release "openfaas" does not exist. Installing it now.
NAME: openfaas
LAST DEPLOYED: Tue Dec 22 11:40:42 2020
NAMESPACE: openfaas
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
To verify that openfaas has started, run:

  kubectl -n openfaas get deployments -l "release=openfaas, app=openfaas"
Waiting for deployment spec update to be observed...
Waiting for deployment "prometheus" rollout to finish: 0 out of 1 new replicas have been updated...
Waiting for deployment "prometheus" rollout to finish: 0 of 1 updated replicas are available...
deployment "prometheus" successfully rolled out
Waiting for deployment "gateway" rollout to finish: 0 of 1 updated replicas are available...
deployment "gateway" successfully rolled out
deployment "gateway" successfully rolled out
contrib/run_function.sh: line 30: faas-cli: command not found
Makefile:50: recipe for target 'start-kind' failed
make: *** [start-kind] Error 127
root@caerus-demo:~/openfaas/faas-netes#  kubectl -n openfaas get deployments -l "release=openfaas, app=openfaas"
NAME                READY   UP-TO-DATE   AVAILABLE   AGE
alertmanager        1/1     1            1           3m10s
basic-auth-plugin   1/1     1            1           3m10s
faas-idler          1/1     1            1           3m10s
gateway             1/1     1            1           3m10s
nats                1/1     1            1           3m10s
prometheus          1/1     1            1           3m10s
queue-worker        1/1     1            1           3m10s
root@caerus-demo:~/openfaas/faas-netes#

```
note: if “make start-kind” is previously run, you might need to delete the kind cluster:
```
root@caerus-demo:~/openfaas/faas-netes# kind get clusters
kind
root@caerus-demo:~/openfaas/faas-netes# kind delete cluster
Deleting cluster "kind" ...
root@caerus-demo:~/openfaas/faas-netes# make start-kind
```
9.	Forward the gateway to your machine
```
> kubectl rollout status -n openfaas deploy/gateway
> kubectl port-forward -n openfaas svc/gateway 8080:8080 &
```
10. Get generated password and login using faas-cli:
```
> PASSWORD=$(kubectl get secret -n openfaas basic-auth -o jsonpath="{.data.basic-auth-password}" | base64 --decode; echo)
>  echo -n $PASSWORD | faas-cli login --username admin --password-stdin
```
11.	Check openfaas GUI via a web browser: 127.0.0.1:8080
A user credential popup window will appear, give following:
user name: admin
password: => to get it using "echo $PASSWORD" by following step 10.
12.	Optional: install arcade: https://github.com/alexellis/arkade
```
> curl -sLS https://dl.get-arkade.dev | sudo sh
> arkade –help
```

# Faas Function Build, Deploy and Invoke

1. To create a new faas function, first check if the programing language is supported by openfaas default:
``` 
root@caerus-demo:/home/yong/caerus/ndp/udf/faas# faas template store list

NAME                     SOURCE             DESCRIPTION
csharp                   openfaas           Classic C# template
dockerfile               openfaas           Classic Dockerfile template
go                       openfaas           Classic Golang template
java8                    openfaas           Java 8 template
java11                   openfaas           Java 11 template
java11-vert-x            openfaas           Java 11 Vert.x template
node12                   openfaas           HTTP-based Node 12 template
node                     openfaas           Classic NodeJS 8 template
php7                     openfaas           Classic PHP 7 template
python                   openfaas           Classic Python 2.7 template
python3                  openfaas           Classic Python 3.6 template
python3-dlrs             intel              Deep Learning Reference Stack v0.4 for ML workloads
ruby                     openfaas           Classic Ruby 2.5 template
ruby-http                openfaas           Ruby 2.4 HTTP template
python27-flask           openfaas           Python 2.7 Flask template
python3-flask            openfaas           Python 3.7 Flask template
python3-flask-debian     openfaas           Python 3.7 Flask template based on Debian
python3-http             openfaas           Python 3.7 with Flask and HTTP
python3-http-debian      openfaas           Python 3.7 with Flask and HTTP based on Debian
golang-http              openfaas           Golang HTTP template
golang-middleware        openfaas           Golang Middleware template
python3-debian           openfaas           Python 3 Debian template
powershell-template      openfaas-incubator Powershell Core Ubuntu:16.04 template
powershell-http-template openfaas-incubator Powershell Core HTTP Ubuntu:16.04 template
rust                     booyaa             Rust template
crystal                  tpei               Crystal template
csharp-httprequest       distantcam         C# HTTP template
csharp-kestrel           burtonr            C# Kestrel HTTP template
vertx-native             pmlopes            Eclipse Vert.x native image template
swift                    affix              Swift 4.2 Template
lua53                    affix              Lua 5.3 Template
vala                     affix              Vala Template
vala-http                affix              Non-Forking Vala Template
quarkus-native           pmlopes            Quarkus.io native image template
perl-alpine              tmiklas            Perl language template based on Alpine image
crystal-http             koffeinfrei        Crystal HTTP template
rust-http                openfaas-incubator Rust HTTP template
bash-streaming           openfaas-incubator Bash Streaming template
cobol                    devries            COBOL Template

root@caerus-demo:/home/yong/caerus/ndp/udf/faas#

``` 
2. If the language support is not there, follow the examples of scala and spring boot examples in the template and examples folders under this directory:
``` 
root@caerus-demo:/home/yong/caerus/ndp/udf/faas# ls
examples  template
``` 
3. To support a custom function template that has different language (using scala as an example, you can see example source code for scala and spring boot under examples folder) or different build systems other than default gradle), here are the steps:
Note:
a. It will be better to copy the "template" folder from Step 2 to a temp folder (be careful to use /tmp folder, the code might not survive reboot), make the proper changes in source code, then copy back to proper git location for the new function
b. several good 'custom' function template examples: 
  * https://blog.alexellis.io/cli-functions-with-openfaas/
  * https://github.com/AnEmortalKid/scala-template-faas
  * https://github.com/tmobile/faas-java-templates
``` 
> mkdir /openfaas 
> cp -r ndp/udf/faas/template/ /openfaas/
> cd /openfaas
> faas new --list
> vi ~/.bashrc
=>>>>> add one line: export OPENFAAS_PREFIX=futureweibostonlab
=>>>>> this "futureweibostonlab" is the prefix of our docker hub organization account account, you either use this one or use your personal docker hub account prefix like "ywang529" https://hub.docker.com/
> source ~/.bashrc
> faas new caerus-faas-scala-function --lang scala
=>>>> This will generate a caerus-faas-scala-function.yml and a source code folder: caerus-faas-scala-function
> faas build -f caerus-faas-scala-function.yml
> docker push futureweibostonlab/caerus-faas-scala-function
> faas deploy -f caerus-faas-scala-function.yml
>  kubectl get pods --all-namespaces
=>>>>>> make sure the function is fully running, and the openfaas GUI should have "ready" state for this function
>  kubectl describe pod caerus-faas-spring-function-665968965f-kd4dg  -n openfaas-fn
=>>>>>> if the function state is an error, troubleshoot the logs for pod and docker
> kubectl logs caerus-faas-spring-function-665968965f-kd4dg -n openfaas-fn
> echo Hi | faas invoke caerus-faas-scala-function
=>>>>>> Or use openfaas GUI to set input params and invoke
```
