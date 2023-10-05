pipeline {

  environment {
    devRegistry = 'ghcr.io/datakaveri/ogc-rs-dev'
    deplRegistry = 'ghcr.io/datakaveri/ogc-rs-depl'
    testRegistry = 'ghcr.io/datakaveri/ogc-rs-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }

  agent { 
    node {
      label 'slave1'
    }
  }

  stages {

    stage('Build images') {
      steps{
        script {
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Unit Tests and Code Coverage Test'){
      steps{
        script{
          sh 'docker compose -f docker-compose.test.yml up test'
        }
        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
          xunit (
            thresholds: [ skipped(failureThreshold: '10'), failed(failureThreshold: '10') ],
            tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
          )
          jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java'
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'sudo rm -rf target/'
          }
        }        
      }
    }

    stage('Start ogc-Resource-Server for Integration Testing'){
      steps{
        script{
          sh 'scp Jmeter/OGCResourceServer.jmx jenkins@jenkins-master:/var/lib/jenkins/iudx/ogc/Jmeter/'
          sh 'scp src/test/resources/OGC_Resource_Server_v0.0.2.postman_collection.json jenkins@jenkins-master:/var/lib/jenkins/iudx/ogc/Newman/'
          sh 'docker compose -f docker-compose.test.yml up -d perfTest'
          sh 'sleep 120'
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
        }
      }
    }

    stage('Jmeter Performance Test'){
      steps{
        node('built-in') {
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/ogc/Jmeter/report ; mkdir -p /var/lib/jenkins/iudx/ogc/Jmeter/report'
            sh "set +x;/var/lib/jenkins/apache-jmeter/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/ogc/Jmeter/OGCResourceServer.jmx -l /var/lib/jenkins/iudx/ogc/Jmeter/report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/ogc/Jmeter/report/ -Jhost=jenkins-slave1"
          }
          perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/ogc/Jmeter/report/*.jtl'     
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml  down --remove-orphans'
          }
        }
      }
    }

    stage('Integration Tests and OWASP ZAP pen test'){
      steps{
        node('built-in') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
            sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/ogc/Newman/OGC_Resource_Server_v0.0.2.postman_collection.json -e /home/ubuntu/configs/ogc-postman-env.json -n 2 --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/ogc/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('built-in') {
            script{
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/ogc/Newman/report/', reportFiles: 'report.html', reportTitles: '', reportName: 'Integration Test Report'])
                archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
              }
            }
          }
        }
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          } 
        }
      }
    }

    stage('Continuous Deployment') {
      when {
        allOf {
          anyOf {
            changeset "docker/**"
            changeset "docs/**"
            changeset "pom.xml"
            changeset "src/main/**"
            triggeredBy cause: 'UserIdCause'
          }
          expression {
            return env.GIT_BRANCH == 'origin/main';
          }
        }
      }
      stages {
        stage('Build image locally in swarm') {
          steps{
            script{
              sh "ssh ubuntu@adex-swarm 'cd /home/ubuntu/ogc-resource-server; git pull;'"
              sh "ssh ubuntu@adex-swarm 'cd /home/ubuntu/ogc-resource-server; docker build -t iudx/ogc-rs-dev:latest -f docker/dev.dockerfile .'"
            }
          }
        }
        stage('Deploy ogc-resource-server') {
          steps{
            script{
              sh "ssh ubuntu@adex-swarm 'docker service update ogc-rs_ogc-rs --force'"
            }
          }
        }
      }
    }
  }
}
