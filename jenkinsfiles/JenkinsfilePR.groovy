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
        stage('Checkout Code') {
            steps {
                script {
                    echo "检出代码..."
                    // Multibranch Pipeline 会自动处理代码检出
                    // 我们只需要验证代码是否已检出

                    // ========== 关键修改：移除 dir('src') 包装 ==========
                    sh 'git log -1 --oneline'
                    sh 'git branch -a'
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