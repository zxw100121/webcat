import java.io.File;
import java.util.Collection;

import eu.medsea.mimeutil.MimeUtil;

public class Main {
    public static void main(String[] args) {
        MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
        File f = new File ("F:/a.jpg");
        Collection<?> mimeTypes = MimeUtil.getMimeTypes(f);
        System.out.println(mimeTypes);
        //  output : application/msword
    }
}