package test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Read and parse Cron-like specification
 *
 */
public class App 
{
    private static final Log LOG = LogFactory.getLog(App.class);

    private static Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSSZ");
        gson = builder
                .setPrettyPrinting()
                .create();
    }

    public static void main( String[] args ) { System.out.println("Hello World!!"); }

    public List<RangerValiditySchedule> getValiditySchedules(String fileName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> init()" );
        }

        List<RangerValiditySchedule> ret = null;

        Reader reader = null;

        URL validitySchedulesURL = null;

        try {
            validitySchedulesURL = getInputFileURL(fileName);

            InputStream in = validitySchedulesURL.openStream();

            reader = new InputStreamReader(in, Charset.forName("UTF-8"));

            Type listType = new TypeToken<List<RangerValiditySchedule>>() {
            }.getType();

            ret = gson.fromJson(reader, listType);

        }
        catch (Exception excp) {
            LOG.error("Error opening request data stream or loading load request data from file, URL=" + validitySchedulesURL, excp);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception excp) {
                    LOG.error("Error closing file ", excp);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== init() : " + ret );
        }
        return ret;
    }

    public static URL getInputFileURL(final String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getInputFileURL(" + name + ")");
        }
        URL ret = null;
        InputStream in = null;


        if (StringUtils.isNotBlank(name)) {

            File f = new File(name);

            if (f.exists() && f.isFile() && f.canRead()) {
                try {

                    in = new FileInputStream(f);
                    ret = f.toURI().toURL();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("URL:" + ret);
                    }

                } catch (FileNotFoundException exception) {
                    LOG.error("Error processing input file:" + name + " or no privilege for reading file " + name, exception);
                } catch (MalformedURLException malformedException) {
                    LOG.error("Error processing input file:" + name + " cannot be converted to URL " + name, malformedException);
                }
            } else {

                URL fileURL = App.class.getResource(name);
                if (fileURL == null) {
                    if (!name.startsWith("/")) {
                        fileURL = App.class.getResource("/" + name);
                    }
                }

                if (fileURL == null) {
                    fileURL = ClassLoader.getSystemClassLoader().getResource(name);
                    if (fileURL == null) {
                        if (!name.startsWith("/")) {
                            fileURL = ClassLoader.getSystemClassLoader().getResource("/" + name);
                        }
                    }
                }

                if (fileURL != null) {
                    try {
                        in = fileURL.openStream();
                        ret = fileURL;
                    } catch (Exception exception) {
                        LOG.error(name + " cannot be opened:", exception);
                    }
                } else {
                    LOG.warn("Error processing input file: URL not found for " + name + " or no privilege for reading file " + name);
                }
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getInputFileURL(" + name + ", URL=" + ret + ")");
        }
        return ret;
    }
}
