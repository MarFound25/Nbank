# Kubernetes + Helm homework — results

Branch: `homework/kubernetes`  
Cluster: Minikube (`--driver=docker`)  
Helm release: `nbank` (`infra/kube/nbank-chart`)

## 1. Services and ports

| Service | Type | Cluster / target port | NodePort (external on node) | Local access via port-forward |
|---------|------|------------------------|-----------------------------|-------------------------------|
| backend | NodePort | 4111 | 30111 | `http://localhost:4111` |
| frontend | NodePort | 80 | 30080 | `http://localhost:3000` |
| postgres | ClusterIP | 5432 | — | internal only |
| wiremock | ClusterIP | 8080 | — | internal (`FRAUD_DETECTION_SERVICE_URL`) |
| selenoid | NodePort | 4444 | 30444 | `http://localhost:4444` |
| selenoid-ui | NodePort | 8080 | 30808 | `http://localhost:8080` |

## 2. Helm deploy

```text
helm upgrade --install nbank ./nbank-chart

NAME 	NAMESPACE	REVISION	UPDATED                              	STATUS  	CHART      	APP VERSION
nbank	default  	1       	2026-07-17 13:42:31.4071676 +0300 MSK	deployed	nbank-0.0.1	1.0.0
```

## 3. kubectl get svc

```text
NAME          TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
backend       NodePort    10.101.197.68   <none>        4111:30111/TCP   3m23s
frontend      NodePort    10.99.165.31    <none>        80:30080/TCP     3m23s
kubernetes    ClusterIP   10.96.0.1       <none>        443/TCP          3m50s
postgres      ClusterIP   10.101.208.56   <none>        5432/TCP         3m23s
selenoid      NodePort    10.104.109.7    <none>        4444:30444/TCP   3m23s
selenoid-ui   NodePort    10.102.174.17   <none>        8080:30808/TCP   3m23s
wiremock      ClusterIP   10.98.208.65    <none>        8080/TCP         3m23s
```

## 4. kubectl get pods (all Running)

```text
NAME                           READY   STATUS    RESTARTS   AGE     IP           NODE
backend-fb9fd4695-n5wml        1/1     Running   0          3m22s   10.244.0.6   minikube
frontend-7cbbcf66fb-cvrjr      1/1     Running   0          3m22s   10.244.0.7   minikube
postgres-5c74f87768-tgthg      1/1     Running   0          3m22s   10.244.0.3   minikube
selenoid-8474b597d9-sk964      1/1     Running   0          3m22s   10.244.0.8   minikube
selenoid-ui-54bc7448f5-z4htg   1/1     Running   0          3m22s   10.244.0.5   minikube
wiremock-6f65b5965f-5wwcw      1/1     Running   0          3m22s   10.244.0.4   minikube
```

## 5. ConfigMaps

### `selenoid-config`
- Managed by Helm (`templates/selenoid-configmap.yaml`)
- Data: `browsers.json` (Chrome / Firefox / Opera images for Selenoid)
- Mounted into Selenoid pod at `/etc/selenoid`

### `postgres-init`
- Contains `01-init-db.sql` (schema bootstrap)
- Mounted into Postgres at `/docker-entrypoint-initdb.d`

### Secrets
No Kubernetes Secrets in this chart. Postgres credentials are set via Deployment env for local homework (user/password `postgres`). Production would use Secrets / sealed-secrets.

## 6. Logs (summary)

- **backend**: Spring Boot started on port 4111, Flyway migrations applied, Hikari connected to `jdbc:postgresql://postgres:5432/nbank`
- **frontend**: nginx started successfully
- **postgres**: ready to accept connections on 5432
- **wiremock**: listening on 8080
- **selenoid / selenoid-ui**: Running; UI points to `http://selenoid:4444`

Commands used:

```bash
kubectl logs deployment/backend --tail=50
kubectl logs deployment/frontend --tail=20
kubectl logs deployment/postgres --tail=20
kubectl logs deployment/wiremock --tail=15
kubectl logs deployment/selenoid --tail=20
kubectl logs deployment/selenoid-ui --tail=15
```

## 7. Port-forward smoke checks

```bash
kubectl port-forward svc/frontend 3000:80
kubectl port-forward svc/backend 4111:4111
kubectl port-forward svc/selenoid 4444:4444
kubectl port-forward svc/selenoid-ui 8080:8080
```

Results:

```text
http://localhost:3000 -> 200
http://localhost:4111/v3/api-docs -> 200
http://localhost:4444/status -> 200
http://localhost:8080 -> 200
```

## 8. Scaling (`kubectl scale`)

```bash
kubectl scale deployment/backend --replicas=2
kubectl get pods -l app=backend
```

After scale: **2/2 backend pods Ready** (`backend-fb9fd4695-n5wml`, `backend-fb9fd4695-rkxss`).

## 9. How to reproduce locally

1. Start Docker Desktop
2. Install Minikube + Helm + kubectl
3. From repo:

```bash
cd infra/kube
# Linux/macOS:
./restart_kube.sh
# Windows PowerShell:
.\restart_kube.ps1
```

Or manually:

```bash
minikube start --driver=docker
helm upgrade --install nbank ./nbank-chart
kubectl get pods
```

## Notes

- Backend image: `nobugsme/nbank:with_database` (lecture swagger-tagged images were not pullable from Docker Hub at homework time).
- Selenoid mounts host Docker socket (`/var/run/docker.sock`) so browser containers can be started by Selenoid inside Minikube.
