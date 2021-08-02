pipeline {
  agent {
    docker {
      args '-v $HOME/.m2:/root/squote-m2'
      image 'openjdk:13-jdk-alpine'
    }
  }

  stages {
    stages {
      stage('resolve dependency') {
        steps {
          sh './mvnw org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=lib/binance-api-client-1.0.1.jar'
          sh './mvnw dependency:go-offline'
        }
      }
      script {
        node {
          docker.image('mongo').withRun('') { c ->
              docker.image('mysql').inside("--link ${c.id}:db") {
                  /* Wait until mysql service is up */
                  sh 'while ! mysqladmin ping -hdb --silent; do sleep 1; done'
              }
          }
        }
      }
      stage('build and unit test') {
        steps {
          sh './mvnw resources:resources package'
        }
      }
    }
  }
}
