pipeline {

  environment {
    devRegistry = 'ghcr.io/datakaveri/geoserver-dev'
    testRegistry = 'ghcr.io/datakaveri/geoserver-test:latest'
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

    stage('Setup Server for Compliance Tests and Code Coverage Test'){
      steps{
        script{
          sh 'scp src/test/resources/OGC_compliance/compliance.xml jenkins@jenkins-master:/var/lib/jenkins/iudx/ogc/'
          sh 'docker compose -f docker-compose.test.yml up -d test'
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

    stage('Start Compliance Tests and Code Coverage Test'){
      steps{
        node('built-in') {
          script{
          //  startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
          //  sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
          //    sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/ogc/Newman/OGC_Resource_Server_v0.0.2.postman_collection.json -e /home/ubuntu/configs/ogc-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/ogc/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
          //    runZapAttack()
          sh '[ ! -d 'ets-ogcapi-features10' ] && git clone https://github.com/opengeospatial/ets-ogcapi-features10'
          sh 'cd ets-ogcapi-features10/'
          sh '[ ! -d 'target' ] && mvn clean package -Dmaven.test.skip -Dmaven.javadoc.skip=true'

          sh 'java -jar target/ets-ogcapi-features10-1.8-SNAPSHOT-aio.jar --generateHtmlReport true /var/lib/jenkins/iudx/ogc/compliance.xml'
            }
          }
        }
      }
      post{
        always{
          node('built-in') {
            script{
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                sh 'cat compliance.xml'
                sh 'cat output'
                  environment {
                      NEWEST_TEST_DIR = sh(script: "ls -t ~/testng | head -n1 | xargs realpath", returnStdout: true).trim()
                  }
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: env.NEWEST_TEST_DIR, reportFiles: 'index.html', reportTitles: '', reportName: 'OGC Compliance Test Report'])
            //    archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 46
              }
            }
          }
        }
        cleanup{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
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
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
              sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/ogc/Newman/OGC_Resource_Server_v0.0.2.postman_collection.json -e /home/ubuntu/configs/ogc-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/ogc/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
              runZapAttack()
            }
          }
        }
      }
      post{
        always{
          node('built-in') {
            script{
              catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/ogc/Newman/report/', reportFiles: 'report.html', reportTitles: '', reportName: 'Integration Test Report'])
                archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 46
              }
            }
          }
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
        stage('Push Images') {
          steps {
            script {
              docker.withRegistry( registryUri, registryCredential ) {
                devImage.push("1.0.0-alpha-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Deploy ogc-resource-server') {
          steps{
            script{
              sh "ssh ubuntu@adex-swarm 'docker service update ogc-rs_ogc-rs --image ghcr.io/datakaveri/geoserver-dev:1.0.0-alpha-${env.GIT_HASH}'"
            }
          }
        }
      }
    }
  }
}
