@Library('jenkins-pipeline-library@master')_

// ========== 修改点1：添加详细的事件检测 ==========
echo "=== PR Pipeline 事件检测 ==="
echo "CHANGE_ID: ${env.CHANGE_ID}"
echo "BRANCH_NAME: ${env.BRANCH_NAME}"
echo "GIT_BRANCH: ${env.GIT_BRANCH}"

// 获取构建原因
def causes = currentBuild.getBuildCauses()
echo "构建原因:"
causes.each { cause ->
    echo " - ${cause}"
}

// 检查是否是 PR 事件
def isPR = env.CHANGE_ID != null
def hasPRCause = causes.any { cause ->
    cause?._class?.contains('GitHubPR') || cause?._class?.contains('PullRequest')
}

echo "isPR (CHANGE_ID != null): ${isPR}"
echo "hasPRCause: ${hasPRCause}"

// 如果不是 PR 事件，友好地跳过
if (!isPR && !hasPRCause) {
    echo "⚠️ 这不是 PR 事件，跳过 PR pipeline 执行"
    currentBuild.result = 'NOT_BUILT'
    return
}

properties([
        parameters([
                string(name: 'PROJECT_NAME', defaultValue: 'demo-helloworld', description: '项目名称'),
                string(name: 'EMAIL_RECIPIENTS', defaultValue: '251934304@qq.com', description: '邮件接收人'),
                // PR 流水线特定参数
                booleanParam(name: 'SKIP_DEPENDENCY_CHECK', defaultValue: true, description: '跳过依赖检查以加速构建（默认跳过）'),
                choice(name: 'SCAN_INTENSITY', choices: ['fast', 'standard', 'deep'], description: '安全扫描强度')
        ]),
        // ========== 修改点2：移除有问题的 GitHubPRTrigger，使用标准触发器 ==========
//        pipelineTriggers([
//                [
//                        $class: 'GitHubPushTrigger',
//                        adminlist: '',
//                        allowWhiteList: false,
//                        branchRestriction: '',      // 空字符串，不限制分支
//                        cron: '',
//                        triggerForBranch: false,    // 禁用分支触发
//                        triggerForPr: true,         // 启用 PR 触发
//                        whiteList: ''
//                ]
//        ])
])

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