{{/*
Expand the name of the chart.
*/}}
{{- define "coinsight-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "coinsight-platform.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "coinsight-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "coinsight-platform.labels" -}}
helm.sh/chart: {{ include "coinsight-platform.chart" . }}
{{ include "coinsight-platform.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: coinsight-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "coinsight-platform.selectorLabels" -}}
app.kubernetes.io/name: {{ include "coinsight-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "coinsight-platform.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "coinsight-platform.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Generate microservice labels for a specific service
*/}}
{{- define "microservice.labels" -}}
app.kubernetes.io/name: {{ .serviceName }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/component: microservice
app.kubernetes.io/part-of: coinsight-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ include "coinsight-platform.chart" .root }}
{{- end }}

{{/*
Generate microservice selector labels
*/}}
{{- define "microservice.selectorLabels" -}}
app.kubernetes.io/name: {{ .serviceName }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Generate database host for a service
*/}}
{{- define "database.host" -}}
{{- if .Values.global.database.external }}
{{- .Values.global.database.host }}
{{- else }}
{{- printf "%s-postgres-%s" (include "coinsight-platform.fullname" .) .serviceName }}
{{- end }}
{{- end }}

{{/*
Generate common environment variables for all microservices
*/}}
{{- define "microservice.env" -}}
- name: SPRING_PROFILES_ACTIVE
  value: "kubernetes"
- name: KEYCLOAK_URL
  value: "http://{{ include "coinsight-platform.fullname" . }}-keycloak:8080"
- name: KEYCLOAK_REALM
  value: "coinsight-realm"
- name: KAFKA_SERVERS
  value: "{{ include "coinsight-platform.fullname" . }}-kafka:9092"
- name: REDIS_HOST
  value: "{{ include "coinsight-platform.fullname" . }}-redis-master"
- name: REDIS_PORT
  value: "6379"
- name: AUTH_SERVICE_URL
  value: "http://auth-service:8081"
- name: TRANSACTION_SERVICE_URL
  value: "http://transaction-service:8082"
- name: OCR_SERVICE_URL
  value: "http://ocr-service:8083"
- name: BUDGET_SERVICE_URL
  value: "http://budget-service:8084"
- name: NOTIFICATION_SERVICE_URL
  value: "http://notification-service:8085"
{{- end }}
