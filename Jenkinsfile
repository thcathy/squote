pipeline {
  agent {
    docker {
      args '-v $HOME/.m2:$HOME/.m2 --network jenkins_build'
      image 'cimg/openjdk:13.0'
    }
  }

  stages {

    stage('resolve dependency') {
      steps {
        sh 'chmod +x mvnw'
        sh './mvnw org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile=lib/binance-api-client-1.0.1.jar'
        sh './mvnw dependency:go-offline'
      }
    }

    stage('build and test') {
      environment {
        APISERVER_HOST = 'https://api.funfunspell.com'
        BINANCE_APIKEY = credentials('BINANCE_APIKEY')
        BINANCE_APISECRET = credentials('BINANCE_APISECRET')
        AUTH0_AUDIENCE = 'testing'
        MONGO_CONTAINER_NAME = "squote-mongo-test-${env.BUILD_NUMBER}"
        MONGO_HOST = "${env.MONGO_CONTAINER_NAME}:27017"
      }

      steps {
        script {
          docker.image('mongo').withRun("-p 27017 --hostname=${env.MONGO_CONTAINER_NAME} --network jenkins_build") { c ->
            sh "docker exec -t ${c.id} bash -c 'while ! pgrep mongod; do sleep 1; done'"
            sh './mvnw package'
            publishHTML (target: [
              reportDir: 'target/site/jacoco/',
              reportFiles: 'index.html',
              reportName: "JaCoCo Report"
            ])
            junit 'target/surefire-reports/**/*.xml'
          }
        }
      }
    }

    stage("Docker build") {
      environment {
        DOCKER_LOGIN = credentials('DOCKER_LOGIN')
        DOCKER_IMAGE_TAG = "${readMavenPom().getVersion()}-${env.BUILD_NUMBER}"
      }
      steps {
        sh "docker build -t thcathy/squote:latest -t thcathy/squote:${DOCKER_IMAGE_TAG} -f docker/Dockerfile ."
        sh "docker login -u $DOCKER_LOGIN_USR -p $DOCKER_LOGIN_PSW"
        sh "docker push thcathy/squote:latest"
        sh "docker push thcathy/squote:${DOCKER_IMAGE_TAG}"
      }
    }

    stage("Deploy to staging") {
      steps {
        sh "docker run -d --rm -p 8765:8080 --name jenkins-squote thcathy/squote"
      }
    }
  }
}
