{{/*
Common labels for microservices
*/}}
{{- define "microservice.labels" -}}
helm.sh/chart: {{ .root.Chart.Name }}-{{ .root.Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ .serviceName }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/version: {{ .root.Chart.AppVersion }}
app.kubernetes.io/managed-by: {{ .root.Release.Service }}
{{- end }}

{{/*
Selector labels for microservices
*/}}
{{- define "microservice.selectorLabels" -}}
app.kubernetes.io/name: {{ .serviceName }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
{{- end }}

{{/*
Microservice deployment template with Kubernetes-native configuration
Usage: {{ include "microservice.deployment" (dict "serviceName" "auth-service" "config" .Values.microservices.authService "root" . "Release" .Release "Chart" .Chart) }}
*/}}
{{- define "microservice.deployment" -}}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .serviceName }}
  namespace: {{ .root.Release.Namespace }}
  labels:
    {{- include "microservice.labels" . | nindent 4 }}
    app.kubernetes.io/part-of: coinsight-platform
    prometheus.io/scrape: "true"
spec:
  replicas: 1
  selector:
    matchLabels:
      {{- include "microservice.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "microservice.selectorLabels" . | nindent 8 }}
        app.kubernetes.io/part-of: coinsight-platform
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "{{ .config.service.port }}"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      # {{- if .config.database }}
      # initContainers:
      # - name: wait-for-database
      #   image: postgres:15-alpine
      #   command: ['sh', '-c']
      #   args:
      #   - |
      #     echo "Waiting for database to be ready..."
      #     until pg_isready -h {{ .config.database.host }}.coinsight.svc.cluster.local -p 5432; do
      #       echo "Database not ready, waiting..."
      #       sleep 5
      #     done
      #     echo "Database is ready!"
      # {{- end }}
      containers:
      - name: {{ .serviceName }}
        image: "{{ .config.image.repository }}:{{ .config.image.tag }}"
        imagePullPolicy: {{ .root.Values.global.image.pullPolicy }}
        ports:
        - containerPort: {{ .config.service.port }}
          name: http
        env:
        # Spring profile
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        # Spring configuration location
        - name: SPRING_CONFIG_LOCATION
          value: "classpath:/application.yml,file:/app/config/application.yml,file:/app/config/{{ .serviceName }}.yml"
        
        # Database configuration from ConfigMaps and Secrets
        {{- if .config.database }}
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: {{ .serviceName }}-db-host
        - name: DB_NAME
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: {{ .serviceName }}-db-name
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: database-secrets
              key: postgres-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-secrets
              key: postgres-password
        {{- end }}
        
        # Keycloak configuration from ConfigMaps and Secrets
        - name: KEYCLOAK_URL
          value: "http://coinsight-platform-keycloak.coinsight.svc.cluster.local"
        - name: KEYCLOAK_REALM
          value: "coinsight-realm"
        {{- if eq .serviceName "auth-service" }}
        - name: KEYCLOAK_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: keycloak-secrets
              key: auth-service-secret
        {{- else if eq .serviceName "transaction-service" }}
        - name: KEYCLOAK_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: keycloak-secrets
              key: transaction-service-secret
        {{- else if eq .serviceName "ocr-service" }}
        - name: KEYCLOAK_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: keycloak-secrets
              key: ocr-service-secret
        {{- else if eq .serviceName "budget-service" }}
        - name: KEYCLOAK_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: keycloak-secrets
              key: budget-service-secret
        {{- else if eq .serviceName "notification-service" }}
        - name: KEYCLOAK_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: keycloak-secrets
              key: notification-service-secret
        {{- else if eq .serviceName "gateway-service" }}
        - name: KEYCLOAK_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: keycloak-secrets
              key: gateway-client-secret
        {{- end }}
        
        # Service URLs from ConfigMaps
        - name: AUTH_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: auth-service-url
        - name: TRANSACTION_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: transaction-service-url
        - name: OCR_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: ocr-service-url
        - name: BUDGET_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: budget-service-url
        - name: NOTIFICATION_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: notification-service-url
        - name: GATEWAY_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: gateway-service-url
        
        # Kafka configuration from ConfigMaps
        - name: KAFKA_SERVERS
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: kafka-servers
        
        # Redis configuration from ConfigMaps
        - name: REDIS_HOST
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: redis-host
        - name: REDIS_PORT
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: redis-port
        
        # Azure configuration (for OCR service)
        {{- if eq .serviceName "ocr-service" }}
        - name: AZURE_FORM_RECOGNIZER_ENDPOINT
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: azure-form-recognizer-endpoint
        - name: AZURE_FORM_RECOGNIZER_API_KEY
          valueFrom:
            secretKeyRef:
              name: azure-secrets
              key: form-recognizer-api-key
        - name: AZURE_OPENAI_ENDPOINT
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: azure-openai-endpoint
        - name: AZURE_OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: azure-secrets
              key: openai-api-key
        - name: AZURE_OPENAI_DEPLOYMENT_ID
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: azure-openai-deployment-id
        {{- end }}
        
        # Mail configuration (for notification service)
        {{- if eq .serviceName "notification-service" }}
        - name: MAIL_HOST
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: mail-host
        - name: MAIL_PORT
          valueFrom:
            configMapKeyRef:
              name: service-config
              key: mail-port
        - name: MAIL_USERNAME
          valueFrom:
            secretKeyRef:
              name: mail-secrets
              key: mail-username
        - name: MAIL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mail-secrets
              key: mail-password
        {{- end }}
        
        # Add monitoring configuration
        - name: MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED
          value: "true"
        - name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
          value: "health,info,metrics,prometheus,loggers"
        
        {{- with .config.env }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: {{ .config.service.port }}
          initialDelaySeconds: 120
          periodSeconds: 30
          timeoutSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: {{ .config.service.port }}
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
        resources:
{{- toYaml .config.resources | nindent 10 }}
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
          readOnly: true
      volumes:
      - name: config-volume
        configMap:
          name: config-repo

{{- end }}

{{/*
Microservice service template
*/}}
{{- define "microservice.service" -}}
---
apiVersion: v1
kind: Service
metadata:
  name: {{ .serviceName }}
  namespace: {{ .root.Release.Namespace }}
  labels:
    {{- include "microservice.labels" . | nindent 4 }}
    app.kubernetes.io/part-of: coinsight-platform
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "{{ .config.service.port }}"
    prometheus.io/path: "/actuator/prometheus"
spec:
  {{- if .config.service.type }}
  type: {{ .config.service.type }}
  {{- end }}
  selector:
    {{- include "microservice.selectorLabels" . | nindent 4 }}
  ports:
  - port: {{ .config.service.port }}
    targetPort: {{ .config.service.port }}
    name: http
    {{- if and (eq .config.service.type "NodePort") .config.service.nodePort }}
    nodePort: {{ .config.service.nodePort }}
    {{- end }}
  {{ "" }}
{{- end }}
