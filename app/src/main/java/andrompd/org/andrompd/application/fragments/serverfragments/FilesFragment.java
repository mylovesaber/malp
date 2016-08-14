package andrompd.org.andrompd.application.fragments.serverfragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.FileAdapter;
import andrompd.org.andrompd.application.callbacks.FABFragmentCallback;
import andrompd.org.andrompd.application.callbacks.PlaylistCallback;
import andrompd.org.andrompd.application.loaders.FilesLoader;
import andrompd.org.andrompd.application.utils.ThemeUtils;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDDirectory;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;

public class FilesFragment extends GenericMPDFragment<List<MPDFileEntry>> implements AbsListView.OnItemClickListener {
    public static final String EXTRA_FILENAME = "filename";
    public static final String TAG = FilesFragment.class.getSimpleName();

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    private FABFragmentCallback mFABCallback = null;

    private FilesCallback mCallback;

    private PlaylistCallback mPlaylistCallback;

    private String mPath;

    /**
     * Adapter used by the ListView
     */
    private FileAdapter mAdapter;

    /**
     * Save the swipe layout for later usage
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.listview_layout_refreshable, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.main_listview);

        Bundle args = getArguments();
        if (null != args) {
            mPath = args.getString(EXTRA_FILENAME);
        } else {
            mPath = "";
        }

        // Create the needed adapter for the ListView
        mAdapter = new FileAdapter(getActivity(), true);

        // Combine the two to a happy couple
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refreshContent();
            }
        });


        // Return the ready inflated and configured fragment view.
        return rootView;
    }

    /**
     * Starts the loader to make sure the data is up-to-date after resuming the fragment (from background)
     */
    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(true, new FABListener());
            if (mPath.equals("")) {
                mFABCallback.setupToolbar(getString(R.string.menu_files), false, true, false);
            } else {
                String[] pathSplit = mPath.split("/");
                if ( pathSplit.length > 0 ) {
                    mFABCallback.setupToolbar(pathSplit[pathSplit.length-1], false, false, false);
                } else {
                    mFABCallback.setupToolbar(mPath, false, false, false);
                }
            }
        }
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (FilesCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPlaylistCallback = (PlaylistCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getActivity().getMenuInflater();
        int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;

        MPDFileEntry file = (MPDFileEntry)mAdapter.getItem(position);

        if (file instanceof MPDFile) {
            inflater.inflate(R.menu.context_menu_track, menu);
        } else if ( file instanceof  MPDDirectory) {
            inflater.inflate(R.menu.context_menu_directory, menu);
        } else if ( file instanceof MPDPlaylist) {
            inflater.inflate(R.menu.context_menu_playlist, menu);
        }
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.action_song_enqueue:
                MPDQueryHandler.addSong(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_song_play:
                MPDQueryHandler.playSong(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_song_play_next:
                MPDQueryHandler.playSongNext(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_play_playlist:
                MPDQueryHandler.playPlaylist(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_add_playlist:
                MPDQueryHandler.loadPlaylist(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_add_directory:
                MPDQueryHandler.addDirectory(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            case R.id.action_play_directory:
                MPDQueryHandler.playDirectory(((MPDFileEntry)mAdapter.getItem(info.position)).getPath());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Creates a new Loader that retrieves the list of playlists
     *
     * @param id
     * @param args
     * @return Newly created loader
     */
    @Override
    public Loader<List<MPDFileEntry>> onCreateLoader(int id, Bundle args) {
        return new FilesLoader(getActivity(), mPath);
    }

    /**
     * When the loader finished its loading of the data it is transferred to the adapter.
     *
     * @param loader Loader that finished its loading
     * @param data   Data that was retrieved by the laoder
     */
    @Override
    public void onLoadFinished(Loader<List<MPDFileEntry>> loader, List<MPDFileEntry> data) {
        mAdapter.swapModel(data);
        // change refresh state
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Resets the loader and clears the model data set
     *
     * @param loader The loader that gets cleared.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDFileEntry>> loader) {
        // Clear the model data of the used adapter
        mAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        MPDFileEntry file = (MPDFileEntry) mAdapter.getItem(i);

        if (file instanceof MPDDirectory) {
            mCallback.openPath(file.getPath());
        } else if (file instanceof MPDPlaylist) {
            mPlaylistCallback.openPlaylist(file.getPath());
        }
    }

    public interface FilesCallback {
        void openPath(String path);
    }

    private class FABListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MPDQueryHandler.playDirectory(mPath);
        }
    }

}
