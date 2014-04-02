job {
    name 'logsearch-website-deploy'
    label('docker')
    logRotator(-1,25)
    scm {
     git {
        remote {
           github('logsearch/logsearch-website')
           credentials("Le-bott's GitHub username/password")
        }
        branch('*/master')
      }
    }
    wrappers {
        colorizeOutput('xterm')
    }
    configure { project ->
      project / buildWrappers / 'EnvInjectPasswordWrapper'(plugin: 'envinject@1.89') << {
        injectGlobalPasswords(true)
      }
    }

    steps {
        shell("""\

cat >run_inside_docker.sh <<EOL 
#!/bin/bash -ex
npm
_build/build.sh
_build/deploy.sh
EOL

chmod +x ./run_inside_docker.sh 

docker build -t \$JOB_NAME \$(pwd)/_build 
docker run -v \$(pwd):/workspace -e CF_USER=sudobot@labs.cityindex.com -e CF_PASSWD=\$SUDOBOT_PASSWORD -e CF_ORG=cityindex-labs -e CF_SPACE=logsearch \$JOB_NAME /workspace/run_inside_docker.sh
""")
    }
}