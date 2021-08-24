pipeline {
  agent {
    docker {
      args '-v $HOME/.m2:$HOME/.m2'
      image 'cimg/openjdk:13.0'
    }
  }

  environment {
    apiserver_host = 'https://homeserver.funfunspell.com/web-parser-rest'
    jasypt_encryptor_password = credentials('JASYPT_ENCRYPTOR_PASSWORD')
    docker_image_tag = "${readMavenPom().getVersion()}-${env.BUILD_NUMBER}"
    DEPLOY_USER = 'thcathy'
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
          }
        }
      }
      post {
        always {
          junit 'target/surefire-reports/**/*.xml'
          publishHTML (target: [
                        reportDir: 'target/site/jacoco/',
                        reportFiles: 'index.html',
                        reportName: "JaCoCo Report"
                      ])
        }
      }
    }

    stage("Docker build") {
      environment {
        DOCKER_LOGIN = credentials('DOCKER_LOGIN')
      }
      steps {
        sh "docker build -t thcathy/squote:latest -t thcathy/squote:${docker_image_tag} -f docker/Dockerfile ."
        sh "docker login -u $DOCKER_LOGIN_USR -p $DOCKER_LOGIN_PSW"
        sh "docker push thcathy/squote:latest"
        sh "docker push thcathy/squote:${docker_image_tag}"
      }
    }

    stage("deploy and verify UAT") {
      when {
        branch 'master'
      }
      agent {
        docker {
          image 'ansible/ansible-runner'
        }
      }
      steps {
        ansiblePlaybook(
          inventory: 'ansible/squote/inventory_uat',
          limit: 'uat',
          extras: "-e docker_image_tag=${docker_image_tag} -e jasypt_encryptor_password=${jasypt_encryptor_password} -e apiserver_host=${apiserver_host}",
          playbook: 'ansible/squote/deploy.yml',
          credentialsId: 'Jenkins-master'
        )
      }
    }

    stage("deploy and verify Prod") {
      when {
        branch 'master'
      }
      agent {
        docker {
          image 'ansible/ansible-runner'
        }
      }
      steps {
        ansiblePlaybook(
          inventory: 'ansible/squote/inventory_prod',
          limit: 'prod',
          extras: "-e docker_image_tag=${docker_image_tag} -e jasypt_encryptor_password=${jasypt_encryptor_password} -e apiserver_host=${apiserver_host}",
          playbook: 'ansible/squote/deploy.yml',
          credentialsId: 'Jenkins-master'
        )
      }
    }
  }

}
