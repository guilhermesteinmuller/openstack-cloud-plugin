package jenkins.plugins.openstack.compute.internal;

import java.util.Collection;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import hudson.model.TaskListener;
import jenkins.plugins.openstack.compute.JCloudsCloud;

import com.google.common.collect.ImmutableMultimap.Builder;

public class TerminateNodes implements Function<Iterable<RunningNode>, Void> {
    private final TaskListener listener;

    public TerminateNodes(TaskListener listener) {
        this.listener = listener;
    }

    public Void apply(Iterable<RunningNode> runningNode) {
        Builder<String, String> cloudNodesToSuspendBuilder = ImmutableMultimap.<String, String>builder();
        Builder<String, String> cloudNodesToDestroyBuilder = ImmutableMultimap.<String, String>builder();
        for (RunningNode cloudTemplateNode : runningNode) {
            cloudNodesToDestroyBuilder.put(cloudTemplateNode.getCloudName(), cloudTemplateNode.getNode().getId());
        }
        Multimap<String, String> cloudNodesToSuspend = cloudNodesToSuspendBuilder.build();
        Multimap<String, String> cloudNodesToDestroy = cloudNodesToDestroyBuilder.build();

        suspendIfSupported(cloudNodesToSuspend);
        destroy(cloudNodesToDestroy);
        return null;
    }

    private void destroy(Multimap<String, String> cloudNodesToDestroy) {
        for (String cloudToDestroy : cloudNodesToDestroy.keySet()) {
            final Collection<String> nodesToDestroy = cloudNodesToDestroy.get(cloudToDestroy);
            listener.getLogger().println("Destroying nodes: " + nodesToDestroy);
            Openstack os = JCloudsCloud.getByName(cloudToDestroy).getOpenstack();
            for (String node: nodesToDestroy) {
                os.destroyServer(node);
            }
        }
    }

    private void suspendIfSupported(Multimap<String, String> cloudNodesToSuspend) {
        for (String cloudToSuspend : cloudNodesToSuspend.keySet()) {
            final Collection<String> nodesToSuspend = cloudNodesToSuspend.get(cloudToSuspend);
            try {
                listener.getLogger().println("Suspending nodes: " + nodesToSuspend);
                Openstack os = JCloudsCloud.getByName(cloudToSuspend).getOpenstack();
                for (String node: nodesToSuspend) {
                    os.suspendServer(node);
                }
            } catch (UnsupportedOperationException e) {
                listener.getLogger().println("Suspending unsupported on cloud: " + cloudToSuspend + "; nodes: " + nodesToSuspend + ": " + e);
            }
        }
    }
}