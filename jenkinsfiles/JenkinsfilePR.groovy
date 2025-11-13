@Library('jenkins-pipeline-library@master')_

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    parameters {
        string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称')
        string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人')
        booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）')
        choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
    }

    stages {
        stage('Checkout via SSH') {
            steps {
                script {
                    echo "使用 SSH 方式检出代码..."

                    // 重试机制
                    retry(3) {
                        checkout([
                                $class: 'GitSCM',
                                branches: [[name: env.BRANCH_NAME]],
                                extensions: [
                                        [$class: 'CleanCheckout'],
                                        [$class: 'RelativeTargetDirectory', relativeTargetDir: 'src'],
                                        [$class: 'CloneOption',
                                         timeout: 5,
                                         depth: 1,
                                         noTags: true,
                                         shallow: true]
                                ],
                                userRemoteConfigs: [[
                                                            url: 'git@github.com:yakiv-liu/demo-helloworld.git',
                                                            credentialsId: 'github-ssh-key-slave'
                                                    ]]
                        ])
                    }

                    dir('src') {
                        sh 'git log -1 --oneline'
                    }
                }
            }
        }

        stage('Run PR Pipeline') {
            steps {
                script {
                    prPipeline([
                            projectName: params.PROJECT_NAME,
                            org: 'yakiv-liu',
                            repo: 'demo-helloworld',
                            agentLabel: 'docker-jnlp-slave',
                            defaultBranch: 'main',
                            defaultEmail: params.EMAIL_RECIPIENTS,
                            skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
                            scanIntensity: params.SCAN_INTENSITY
                    ])
                }
            }
        }
    }
}