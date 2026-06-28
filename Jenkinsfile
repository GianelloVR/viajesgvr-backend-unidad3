pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'gianellovr/viajesgvr-backend'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean test'
            }
        }

        stage('Build Maven Package') {
            steps {
                sh './mvnw package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $DOCKER_IMAGE:latest -t $DOCKER_IMAGE:$BUILD_NUMBER .'
            }
        }

        stage('Push Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKERHUB_USER',
                    passwordVariable: 'DOCKERHUB_TOKEN'
                )]) {
                    sh 'echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin'
                    sh 'docker push $DOCKER_IMAGE:latest'
                    sh 'docker push $DOCKER_IMAGE:$BUILD_NUMBER'
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }

        success {
            echo 'Pipeline ejecutado correctamente.'
        }

        failure {
            echo 'Pipeline falló. Revisar logs de Jenkins.'
        }
    }
}
