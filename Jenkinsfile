pipeline {
    agent {
        docker {
            image 'maven:3.9.6-eclipse-temurin-11'
            args '-v ~/.m2:/root/.m2'
        }
    }
    environment {
        SELENIUM_REMOTE_URL = 'http://selenium-hub:4444/wd/hub'
        ENVIRONMENT = 'remote'
    }
    stages {
        stage('Setup Selenium Grid') {
            steps {
                sh 'docker-compose up -d selenium-hub chrome'
            }
        }
        stage('Run Serenity Tests') {
            steps {
                sh 'mvn clean verify -Denvironment=remote'
            }
        }
    }
    post {
        always {
            sh 'docker-compose down'
        }
    }
}