//package io.nautime.jetbrains.listeners
//
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.startup.StartupActivity
//import git4idea.GitLocalBranch
//import git4idea.GitReference
//import git4idea.branch.GitBranchUtil
//import git4idea.repo.GitRepository
//import java.util.Optional
//import java.util.function.Consumer
//
//open class KaefGitListener : StartupActivity {
//
//    private val logger = Logger.getInstance("GitListener")
//
//    override fun runActivity(project: Project) {
//        project.messageBus.connect().subscribe(GitRepository.GIT_REPO_CHANGE, Any({ repository ->
//            val currentBranch: GitLocalBranch = repository.getCurrentBranch()
//            if (currentBranch != null) {
//                notifyBranchChanged(repository.getProject(), currentBranch.getName())
//            }
//        }))
//        Optional.ofNullable(GitBranchUtil.getCurrentRepository(project))
//            .map<Any>(GitRepository::getCurrentBranch)
//            .map<Any>(GitReference::getName)
//            .ifPresent(Consumer { branchName: Any -> notifyBranchChanged(project, branchName) })
//    }
//
//    private fun notifyBranchChanged(project: Project, branchName: String) {
//        val service: BranchHelper = project.getService(BranchHelper::class.java)
//        if (service == null) {
//            logger.warn("Failed to notify branch change")
//            return
//        }
//        service.onBranchChanged(branchName)
//    }
//}
