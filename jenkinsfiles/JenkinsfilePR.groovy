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
        // ========== 新增：目标分支检查阶段 ==========
        stage('Check PR Target Branch') {
            steps {
                script {
                    echo "检查 PR 目标分支..."
                    echo "当前分支: ${env.BRANCH_NAME}"
                    echo "PR 目标分支: ${env.CHANGE_TARGET}"
                    echo "PR 源分支: ${env.CHANGE_BRANCH}"
                    echo "PR 编号: ${env.CHANGE_ID}"

                    // 定义允许的目标分支
                    def allowedTargetBranches = ['master', 'main']

                    // 检查是否在允许的目标分支列表中
                    if (env.CHANGE_TARGET && allowedTargetBranches.contains(env.CHANGE_TARGET)) {
                        echo "✅ PR 目标分支验证通过: ${env.CHANGE_TARGET}"
                        currentBuild.description = "PR to ${env.CHANGE_TARGET}"
                    } else {
                        echo "⏭️ 跳过 PR - 目标分支 ${env.CHANGE_TARGET} 不在允许列表中"
                        currentBuild.result = 'SUCCESS'
                        currentBuild.description = "跳过 - 目标分支 ${env.CHANGE_TARGET} 不受支持"
                        // 直接结束 pipeline
                        return
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

    // ========== 新增：后处理逻辑 ==========
    post {
        always {
            script {
                echo "PR Pipeline 执行完成 - 结果: ${currentBuild.result}"
                // 清理工作空间
                cleanWs()
            }
        }
    }
}