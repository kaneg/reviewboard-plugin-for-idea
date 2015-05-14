package rb;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: gongze
 * Date: 3/21/2015
 * Time: 4:08 PM
 */
public interface VCSBuilder {

    AbstractVcs getVCS();

    void build(Project project, VirtualFile[] vFiles);

    String getDiff();

    String getRepositoryURL();

    String getBasePath();


    class Factory {
        static Map<String, Class<? extends VCSBuilder>> builders = new HashMap<String, Class<? extends VCSBuilder>>();

        static {
            builders.put("Git", GitVCSBuilder.class);
            builders.put("svn", SVNVCSBuilder.class);
        }

        public static VCSBuilder getBuilder(AbstractVcs vcs) {
            Class<? extends VCSBuilder> aClass = builders.get(vcs.getName());
            if (aClass != null) {
                Constructor<? extends VCSBuilder> constructor = null;
                try {
                    constructor = aClass.getConstructor(AbstractVcs.class);
                    return constructor.newInstance(vcs);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
