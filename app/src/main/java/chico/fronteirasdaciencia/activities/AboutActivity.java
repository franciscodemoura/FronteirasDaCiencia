package chico.fronteirasdaciencia.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import chico.fronteirasdaciencia.R;

/**
 * Created by chico on 27/06/2015. Uhu!
 */
public class AboutActivity extends Activity {

    public final static String PODCAST_DATA_TAG = "podcast_data";

    @Override
    protected void onCreate(Bundle saved_instance){
        super.onCreate(saved_instance);
        setContentView(R.layout.about_layout);

        ((TextView) findViewById(R.id.podcast_title)).setText(getIntent().getStringArrayExtra(PODCAST_DATA_TAG)[0]);
        ((TextView) findViewById(R.id.podcast_link)).setText(getIntent().getStringArrayExtra(PODCAST_DATA_TAG)[1]);
        ((TextView) findViewById(R.id.podcast_description)).setText(getIntent().getStringArrayExtra(PODCAST_DATA_TAG)[2].replaceAll("[\n\r\t]", " ").replaceAll("[ ]{2,}"," "));

        findViewById(R.id.podcast_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String url = getIntent().getStringArrayExtra(PODCAST_DATA_TAG)[1];
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });
    }
}
