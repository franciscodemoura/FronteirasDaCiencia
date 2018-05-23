package chico.fronteirasdaciencia.services.episode_service;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import chico.fronteirasdaciencia.R;
import chico.fronteirasdaciencia.aidl.EpisodeData;

/**
 * Created by chico on 07/06/2015. Uhu!
 */

class EpisodeDataManager {

    public interface StreamProgressListener {
        boolean setProgress(int max, int progress);
    }

    private volatile List<EpisodeData> mEpisodeList = new ArrayList<>();
    private final Context mContext;
    private final String mEpisodeGuideFilename;
    private final String mEpisodeGuideTempFilename;
    private final String mEpisodeGuideURL;
    private final EpisodeEventsInterface mServiceEvents;
    private final File mEpisodesDirectory;
    private final SharedPreferences mViewedEpisodes;
    private final String mEpisodeViewedPrefix;
    private String mPodcastTitle;

    public synchronized String getPodcastTitle() {
        return mPodcastTitle;
    }

    public synchronized String getPodcastDescription() {
        return mPodcastDescription;
    }

    public synchronized String getPodcastLink() {
        return mPodcastLink;
    }

    private String mPodcastDescription;
    private String mPodcastLink;


    public EpisodeDataManager(final Context context, final EpisodeEventsInterface service_events){
        mContext = context;
        mServiceEvents = service_events;
        mEpisodeGuideFilename =  mContext.getResources().getString(R.string.episode_guide_filename);
        mEpisodeGuideTempFilename =  mContext.getResources().getString(R.string.episode_guide_temp_filename);
        mEpisodeGuideURL =  mContext.getResources().getString(R.string.episode_guide_URL);
        mEpisodesDirectory = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PODCASTS),context.getString(R.string.fdac_directory));
        mEpisodesDirectory.mkdir();
        mViewedEpisodes = context.getSharedPreferences(mContext.getResources().getString(R.string.fdac_viewed_preference_file_name),Context.MODE_PRIVATE);
        mEpisodeViewedPrefix = mContext.getResources().getString(R.string.fdac_viewed_episode_prefix);
        loadEpisodeList();
    }

    private void loadEpisodeList(){
        synchronized (mContext) {
            if (!doesEpisodeGuideFileExist()) {
                loadDefaultEpisodeGuide();
            }
            setEpisodeList(getEpisodeListFromEpisodeGuideFile());
        }
    }

    public int downloadAndUpdateEpisodeGuide(){
        if(downloadXMLEpisodeGuide()){
            return mergeEpisodeList(getEpisodeListFromEpisodeGuideFile());
        }
        else{
            return 0;
        }
    }

    public synchronized List<EpisodeData> getEpisodeList(){
        //return mEpisodeList;
        return new ArrayList<EpisodeData> (mEpisodeList);
    }

    public synchronized int size(){
        return mEpisodeList.size();
    }

    private synchronized EpisodeData getEpisode(final long episode_id) {
        return mEpisodeList.get((int) episode_id - 1);
    }

    public synchronized EpisodeData getEpisode(final int episode_index){
        return mEpisodeList.get(episode_index);
    }

    public synchronized void episodeDownloading(final long episode_id){
        getEpisode(episode_id).setState(EpisodeData.EpisodeState.DOWNLOADING);
        mServiceEvents.episodeDownloading(episode_id);
    }

    public synchronized void episodeDownloaded(final long episode_id, final Uri local_file){
        final EpisodeData episode = getEpisode(episode_id);
        episode.setState(EpisodeData.EpisodeState.DOWNLOADED);
        episode.setLocalFile(local_file);
        mServiceEvents.episodeDownloaded(episode_id, local_file);
    }

    public synchronized void episodeAbsent(final long episode_id){
        getEpisode(episode_id).setState(EpisodeData.EpisodeState.ABSENT);
        mServiceEvents.episodeAbsent(episode_id);
    }

    public synchronized void episodeViewed(final long episode_id, final boolean viewed){
        getEpisode(episode_id).setViewed(viewed);
        mViewedEpisodes.edit().putBoolean(mEpisodeViewedPrefix+episode_id,viewed).apply();
        mServiceEvents.episodeViewed(episode_id,viewed);
    }

    public boolean downloadEpisode(final long episode_id, final StreamProgressListener progress_listener){
        final EpisodeData episode = getEpisode(episode_id);
        try {
            return writeInputStreamToFile(
                    (InputStream) new URL(episode.getUrl().toString()).getContent(),
                    new File(
                            mEpisodesDirectory,
                            mContext.getString(R.string.fdac_filename_prefix) + episode_id + ".mp3"
                    ),
                    1024*256,
                    progress_listener,
                    episode.getFileSize()
            );
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteEpisodeFile(final long episode_id){
        return new File(mEpisodesDirectory,mContext.getString(R.string.fdac_filename_prefix) + episode_id + ".mp3").delete(); //TODO-use the suffix from the url file
    }

    public Uri checkEpisodeFile(final long episode_id){
        //final EpisodeData episode = getEpisode(episode_id);
        final File episode_file = new File(mEpisodesDirectory,mContext.getString(R.string.fdac_filename_prefix) + episode_id + ".mp3");  //TODO-use the suffix from the url file
        return episode_file.exists() ? Uri.parse("file:"+episode_file.getAbsolutePath()) : null;
        //return episode_file.length() == episode.getFileSize() ? Uri.parse("file:"+episode_file.getAbsolutePath()) : null;
    }

    private boolean downloadXMLEpisodeGuide(){
        synchronized (mContext) {
            try {

                final InputStream input_http_stream =
                        (InputStream) new URL(mEpisodeGuideURL).getContent();

                writeInputStreamToFile(
                        input_http_stream,
                        new File(
                                mContext.getCacheDir(),
                                mEpisodeGuideTempFilename
                        ), 1024, null, 0
                );

                if(
                        new File(
                                mContext.getCacheDir(),
                                mEpisodeGuideTempFilename
                        ).length()
                    ==
                        new File(
                                mContext.getFilesDir(),
                                mEpisodeGuideFilename
                        ).length()
                ){
                    return true;
                }

                final InputStream temp_file_input_stream =
                        new FileInputStream(
                                new File(
                                        mContext.getCacheDir(),
                                        mEpisodeGuideTempFilename
                                )
                        );

                writeInputStreamToFile(
                        temp_file_input_stream,
                        new File(
                                mContext.getFilesDir(),
                                mEpisodeGuideFilename
                        ), 1024, null, 0
                );

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private List<EpisodeData> getEpisodeListFromEpisodeGuideFile(){
        final InputStream episode_guide_input_stream;
        try {
            episode_guide_input_stream = mContext.openFileInput(mEpisodeGuideFilename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        try {
            final Document episode_guide_dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(episode_guide_input_stream);
            episode_guide_input_stream.close();
            return extractEpisodeList(episode_guide_dom);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<EpisodeData> extractEpisodeList(final Document document){
        readPodcastDescription(document);
        final ArrayList<EpisodeData> episode_list = new ArrayList<>();

        final NodeList item_list = document.getElementsByTagName("item");
        for(int i=0; i<item_list.getLength(); i++){

            final Element episode_element = (Element) item_list.item(i);
            final Element enclosure_element = (Element) episode_element.getElementsByTagName("enclosure").item(0);

            final String title = episode_element.getElementsByTagName("title").item(0).getTextContent();
            final String description = episode_element.getElementsByTagName("description").item(0).getTextContent();
            final String date = episode_element.getElementsByTagName("pubDate").item(0).getTextContent();
            final Uri url = Uri.parse(enclosure_element.getAttribute("url"));
            final int file_size = Integer.parseInt(enclosure_element.getAttribute("length"));
            final List<String> categories = new ArrayList<>();

            if(i+1 != 70) { //Ignore episode 70
                final long id = (i+1) > 70 ? i : i+1;

                if(id >= 1 && id <= 29){
                    categories.add("Primeira");
                }
                else if(id >= 30 && id <= 68){
                    categories.add("Segunda");
                }
                else if(id >= 69 && id <= 110){
                    categories.add("Terceira");
                }
                else if(id >= 111 && id <= 153){
                    categories.add("Quarta");
                }
                else if(id >= 154 && id <= 195){
                    categories.add("Quinta");
                }
                else if(id >= 196 && id <= 238){
                    categories.add("Sexta");
                }
                else if(id >= 239 && id <= 281){
                    categories.add("Sétima");
                }
                else if(id >= 282 && id <= 322){
                    categories.add("Oitava");
                }
                else if(id >= 323){
                    categories.add("Nona");
                }

                episode_list.add(
                        new EpisodeData(
                                id,
                                title,
                                description,
                                url,
                                date,
                                file_size,
                                mViewedEpisodes.getBoolean(mEpisodeViewedPrefix + id, false),
                                categories
                        )
                );
            }
        }

        return episode_list;
    }

    private synchronized void readPodcastDescription(final Document document){
        final Element channel = (Element) document.getElementsByTagName("channel").item(0);

        Node node = channel.getFirstChild();
        while(node != null){
            if(node.getNodeType() == Node.ELEMENT_NODE){
                final Element element = (Element) node;
                if(element.getTagName().equals("title")){
                    mPodcastTitle = element.getTextContent();
                }
                else if(element.getTagName().equals("link")){
                    mPodcastLink = element.getTextContent();
                }
                else if(element.getTagName().equals("description")){
                    mPodcastDescription = element.getTextContent();
                }
            }
            node = node.getNextSibling();
        }
    }

    private synchronized void setEpisodeList(final List<EpisodeData> episode_list){
        if(episode_list != null){
            mEpisodeList = episode_list;
        }
    }

    private synchronized int mergeEpisodeList(final List<EpisodeData> episode_list){
        if(episode_list != null) {
            final int temp = episode_list.size() - mEpisodeList.size();
            for (int i = mEpisodeList.size(); i < episode_list.size(); i++) {
                mEpisodeList.add(episode_list.get(i));
                mServiceEvents.newEpisode(episode_list.get(i));
            }
            return temp;
        }
        else{
            return 0;
        }
    }

    private boolean doesEpisodeGuideFileExist(){
        try {
            InputStream episode_guide_input_stream = mContext.openFileInput(mEpisodeGuideFilename);
            try {
                episode_guide_input_stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private void loadDefaultEpisodeGuide() {
        final InputStream default_episode_guide_stream =
                mContext.getResources().openRawResource(R.raw.default_episode_guide);

        writeInputStreamToFile(
                default_episode_guide_stream,
                new File(
                        mContext.getFilesDir(),
                        mEpisodeGuideFilename
                ), 1024, null, 0
        );
    }

    private boolean writeInputStreamToFile(final InputStream input_stream, final File output_file, final int block_size, final StreamProgressListener progress_listener, final int stream_size) {

        if(progress_listener != null){
            progress_listener.setProgress(stream_size,0);
        }

        OutputStream episode_guide_output_stream = null;

        try {
            episode_guide_output_stream = new FileOutputStream(output_file);

            final byte[] bytes = new byte[block_size];
            int n, subtotal, total = 0;

            do {
                subtotal = 0;
                while (bytes.length - subtotal > 0 && (n = input_stream.read(bytes, subtotal, bytes.length - subtotal)) > 0) {
                    subtotal += n;
                }
                episode_guide_output_stream.write(bytes, 0, subtotal);
                total += subtotal;

                if (progress_listener != null) {
                    if (progress_listener.setProgress(stream_size, total)) {
                        break;
                    }
                }
            } while (subtotal >= bytes.length);
            return true;
        }
        catch (IOException e){
            return false;
        }
        finally {
            if(episode_guide_output_stream != null){
                try {
                    episode_guide_output_stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                input_stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
