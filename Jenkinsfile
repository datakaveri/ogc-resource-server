pipeline {
  agent { 
    node {
      label 'slave1' 
    }
  }
  stages {
    stage('Build Image') {
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
