@Library('jenkins-pipeline-library@master')_

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）'),
                choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
        ]),
        // ========== 修复：使用正确的 ExpressionType ==========
        pipelineTriggers([
                [
                        $class: 'GenericTrigger',
                        genericVariables: [
                                [
                                        key: 'action',
                                        value: '$.action'
                                        // 移除 expressionType 或者使用正确的值
                                ],
                                [
                                        key: 'pr_number',
                                        value: '$.number'
                                ],
                                [
                                        key: 'pr_state',
                                        value: '$.pull_request.state'
                                ],
                                [
                                        key: 'pr_merged',
                                        value: '$.pull_request.merged'
                                ],
                                [
                                        key: 'head_ref',
                                        value: '$.pull_request.head.ref'
                                ],
                                [
                                        key: 'base_ref',
                                        value: '$.pull_request.base.ref'
                                ],
                                [
                                        key: 'head_sha',
                                        value: '$.pull_request.head.sha'
                                ]
                        ],
                        token: 'demo-helloworld-pr',
                        causeString: 'GitHub PR Triggered',
                        printContributedVariables: true,
                        printPostContent: true,
                        regexpFilterText: '$action',
                        regexpFilterExpression: '^(opened|reopened|synchronize)$',
                        silentResponse: false
                ]
        ])
])

// ========== 基于 Generic Webhook 参数判断 ==========
// 注意：这些变量现在由 Generic Webhook Trigger 自动提供
def isPR = (action == 'opened' || action == 'reopened' || action == 'synchronize') && pr_state == 'open'

echo "=== PR Pipeline 事件检测 ==="
echo "action: ${action}"
echo "pr_number: ${pr_number}"
echo "pr_state: ${pr_state}"
echo "pr_merged: ${pr_merged}"
echo "head_ref: ${head_ref}"
echo "base_ref: ${base_ref}"
echo "head_sha: ${head_sha}"
echo "isPR: ${isPR}"

if (!isPR) {
    echo "⚠️ 这不是 PR 创建/更新事件，跳过 PR pipeline 执行"
    currentBuild.result = 'NOT_BUILT'
    return
}

// 设置 PR 相关的环境变量
env.CHANGE_ID = pr_number
env.CHANGE_BRANCH = head_ref
env.CHANGE_TARGET = base_ref
env.GIT_COMMIT = head_sha

echo "✅ 确认：这是 PR #${pr_number} 事件，继续执行PR流水线"
echo "PR 源分支: ${head_ref}"
echo "PR 目标分支: ${base_ref}"
echo "PR Commit SHA: ${head_sha}"

pipeline {
    agent {
        label 'docker-jnlp-slave'
    }

    stages {
        stage('Run PR Pipeline') {
            steps {
                script {
                    // 调用共享库的PR流水线
                    prPipeline([
                            // 基础配置
                            projectName: params.PROJECT_NAME,
                            org: 'yakiv-liu',
                            repo: 'demo-helloworld',
                            agentLabel: 'docker-jnlp-slave',
                            defaultBranch: 'main',
                            defaultEmail: params.EMAIL_RECIPIENTS,

                            // PR 特定配置
                            skipDependencyCheck: params.SKIP_DEPENDENCY_CHECK.toBoolean(),
                            scanIntensity: params.SCAN_INTENSITY
                    ])
                }
            }
        }
    }
}