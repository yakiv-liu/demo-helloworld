@Library('jenkins-pipeline-library@master')_

// ========== 修改点1：Multibranch Pipeline 不需要复杂的 properties 配置 ==========
// 移除了之前有问题的 properties 块，Multibranch Pipeline 会自动处理分支和PR发现

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    // ========== 修改点2：恢复 parameters 块，但在 Multibranch 中有些参数可能不需要 ==========
    parameters {
        string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称')
        string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人')
        booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）')
        choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
    }

    stages {
        stage('Check PR Event') {
            steps {
                script {
                    echo "=== PR Pipeline 事件检测 ==="
                    echo "CHANGE_ID: ${env.CHANGE_ID}"
                    echo "CHANGE_BRANCH: ${env.CHANGE_BRANCH}"
                    echo "CHANGE_TARGET: ${env.CHANGE_TARGET}"
                    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                    echo "GIT_BRANCH: ${env.GIT_BRANCH}"

                    // 打印所有构建原因
                    def causes = currentBuild.getBuildCauses()
                    echo "构建原因:"
                    causes.each { cause ->
                        echo " - ${cause.shortDescription ?: cause.toString()}"
                    }

                    // ========== 修改点3：简化 PR 检测逻辑，Multibranch 会自动设置环境变量 ==========
                    if (env.CHANGE_ID) {
                        echo "✅ 确认：这是 PR #${env.CHANGE_ID} 事件，继续执行PR流水线"
                        echo "PR 源分支: ${env.CHANGE_BRANCH}"
                        echo "PR 目标分支: ${env.CHANGE_TARGET}"
                    } else {
                        echo "⚠️ 这不是 PR 事件，可能是分支构建"
                        echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                        echo "将继续执行，但某些 PR 特定功能可能无法工作"
                    }
                }
            }
        }

        stage('Run PR Pipeline') {
            steps {
                script {
                    // ========== 修改点4：确保参数传递正确 ==========
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

    // ========== 修改点5：添加 post 部分用于构建后处理 ==========
    post {
        always {
            echo "PR Pipeline 执行完成 - 结果: ${currentBuild.result}"
        }
        success {
            echo "✅ PR Pipeline 执行成功"
        }
        failure {
            echo "❌ PR Pipeline 执行失败"
        }
        unstable {
            echo "⚠️ PR Pipeline 执行不稳定"
        }
    }
}