pipeline {
  agent {
    docker {
      args '-v $HOME/.m2:$HOME/.m2'
      image 'cimg/openjdk:13.0'
    }
  }

  environment {
    APISERVER_HOST = 'https://api.funfunspell.com'
    BINANCE_APIKEY = credentials('BINANCE_APIKEY')
    BINANCE_APISECRET = credentials('BINANCE_APISECRET')
    AUTH0_AUDIENCE = 'testing'
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
      steps {
        script {
          docker.image('mongo').withRun("") { c ->
            script {
                env.MONGO_HOST = sh (
                    script: "echo \$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${c.id}):27017",
                    returnStdout: true
                ).trim()
            }
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
        sh "docker-compose -f docker-compose-test.yaml up -d"
      }
    }

    stage("Acceptance test") {
      steps {
        sh 'chmod +x ./script/bin/*.sh'
        script {
          env.SQUOTE_CID = sh (
              script: 'docker ps -f name=squote_jenkins_squote | grep thcathy/squote | cut -d \' \' -f1',
              returnStdout: true
          ).trim()
          env.SQUOTE_HOST = sh (
              script: "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${env.SQUOTE_CID}",
              returnStdout: true
          ).trim()
        }
        sh 'echo $SQUOTE_HOST'
        sleep 30
        sh "./script/bin/acceptance_test.sh http://${env.SQUOTE_HOST}:8080"
      }
    }

  }

  post {
    always {
      sh "docker-compose -f docker-compose-test.yaml down"
    }
  }
}
