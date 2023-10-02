package com.bilalberek.senfony.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.bilalberek.senfony.R
import com.bilalberek.senfony.Repository.ItunesRepo
import com.bilalberek.senfony.Repository.PodcastRepo
import com.bilalberek.senfony.adapters.PodcastListAdapter
import com.bilalberek.senfony.databinding.ActivityMainBinding
import com.bilalberek.senfony.service.EpisodePlayerFragment
import com.bilalberek.senfony.service.ItunesService
import com.bilalberek.senfony.service.RssFeedService
import com.bilalberek.senfony.ui.fragments.PodcastDetailsFragment
import com.bilalberek.senfony.viewModel.PodcastViewModel
import com.bilalberek.senfony.viewModel.SearchViewModel
import com.bilalberek.senfony.worker.EpisodeUpdateWorker

import kotlinx.coroutines.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit

class PodcastActivity : AppCompatActivity(),PodcastDetailsFragment.OnDetailsFragmentListener {
     val podcastViewModel by viewModels<PodcastViewModel>()
    private  var searchItem : MenuItem? = null
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    val TAG = javaClass.simpleName
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)





        setupToolbar()
        setupViewModels()
        updateControls()
        setUpPodcastListView()
        handleIntent(intent)
        showDetails()
        addBackStackListener()
        createSubscription()
        scheduleJobs()

    }


    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupViewModels() {
        searchViewModel.iTunesRepo = ItunesRepo(ItunesService.instance)
        podcastViewModel.podcastRepo = PodcastRepo(RssFeedService.instance,podcastViewModel.podcastDao)
    }

    private fun updateControls() {
        binding.podcastRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            binding.podcastRecyclerView.context,
            layoutManager.orientation)

        binding.podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(this)
        binding.podcastRecyclerView.adapter = podcastListAdapter
    }




    private fun performSearch(term: String) {
        showProgressBar()

       GlobalScope.async {
            try {
                if (searchViewModel.isInternetAvailable(this@PodcastActivity)){
                    val results = searchViewModel.searchPodcasts(term)
                    withContext(Dispatchers.Main) {
                        hideProgressBar()
                        binding.toolbar.title = term
                        podcastListAdapter.differ.submitList(results)
                }

                }else{
                    Toast.makeText(this@PodcastActivity,
                        "checkout internet connection please",
                        Toast.LENGTH_SHORT).show()

                }
            }catch (t:Throwable){
                when(t){
                    is IOException ->    Toast.makeText(this@PodcastActivity,
                        "checkout internet connection please",
                        Toast.LENGTH_SHORT).show()
                }
            }

        }
    }


    private fun handleIntent(intent: Intent) {

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }

        val podcastFeedUrl =
            intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        supportFragmentManager.popBackStack()

        podcastFeedUrl?.let {
            podcastViewModel.viewModelScope.launch {
                val podcastSummaryViewData  = podcastViewModel.setActivePodcast(podcastFeedUrl)
              showDetails()
            }
        }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i("yarak", "Results = ${intent?.action}")
        setIntent(intent)
        handleIntent(intent?:Intent())
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.search_menu, menu)

         searchItem = menu.findItem(R.id.search_item)

        searchItem?.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
        override fun onMenuItemActionCollapse(item: android.view.MenuItem): Boolean{
            showSubscribedPodcasts()
            return true
            }

            override fun onMenuItemActionExpand(item: android.view.MenuItem): Boolean{
                return true
            }
        })


        val searchView = searchItem?.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE)
                as SearchManager

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (supportFragmentManager.backStackEntryCount > 0) {
            binding.podcastRecyclerView.visibility = View.INVISIBLE
        }

        if (binding.podcastRecyclerView.visibility == View.INVISIBLE) {
            searchItem?.isVisible = false
        }


        return true
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }


    companion object{
        private const val TAG_EPİSODE_UPDATE_JOB = "com.bilalberek.senfony.ui"
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    }


    private fun createPodcastDetailFragment(): PodcastDetailsFragment{
        var podcastDetailFragment = supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT)
                as? PodcastDetailsFragment

        if(podcastDetailFragment == null){
            podcastDetailFragment  = PodcastDetailsFragment.newInstance()
        }

       return podcastDetailFragment
    }

    private fun showDetailsFragment(){
        val podcastDetailsFragment = createPodcastDetailFragment()
        if(supportFragmentManager.backStackEntryCount == 0){
            supportFragmentManager.beginTransaction().add(
                R.id.podcastDetailsContainer,
                podcastDetailsFragment,
                TAG_DETAILS_FRAGMENT
            ).addToBackStack("DetailsFragment").commit()

            binding.podcastRecyclerView.visibility = View.INVISIBLE
            searchItem?.isVisible = false
        }




    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }




    @OptIn(DelicateCoroutinesApi::class)
    private fun showDetails(){

        podcastListAdapter.setOnItemClickListener { podcastSummaryViewData ->
            podcastSummaryViewData?.let{
                val feedUrl = podcastSummaryViewData.feedUrl ?: return@setOnItemClickListener
                showProgressBar()
                GlobalScope.launch {
                    podcastViewModel.getPodcast(podcastSummaryViewData)
                }
            } ?: showError("podcastSummaryViewData is null")



        }
    }
    private fun createSubscription() {
        podcastViewModel.podcastLiveData.observe(this, {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed")
                }
            })
        }


    private fun addBackStackListener(){
        supportFragmentManager.addOnBackStackChangedListener {
            if(supportFragmentManager.backStackEntryCount == 0){
                binding.podcastRecyclerView.visibility = View.VISIBLE
                searchItem?.isVisible = true
            }
        }
    }

    override fun onSubscribe(){
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }
    override fun onUnSubscribe(){
        Log.d("bilal","onUnsubscribe")
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
            podcastViewModel.activeEpisodeViewData = episodeViewData
            showPlayerFragment()
    }


    private fun showSubscribedPodcasts(){
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null){
            binding.toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.differ.submitList(podcasts)
        }

    }

    private fun setUpPodcastListView(){
        podcastViewModel.getPodcasts()?.observe(this,{
            if (it != null){
                showSubscribedPodcasts()
            }
        })
    }

    private fun scheduleJobs(){
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()

        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            15,TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(TAG_EPİSODE_UPDATE_JOB,
        ExistingPeriodicWorkPolicy.REPLACE,request)
    }


    private fun createEpisodePlayerFragment(): EpisodePlayerFragment {
        var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as EpisodePlayerFragment?
        if (episodePlayerFragment == null){
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }
        return episodePlayerFragment
    }

    private fun showPlayerFragment(){
        val episodePlayerFragment = createEpisodePlayerFragment()

            supportFragmentManager.beginTransaction().replace(R.id.podcastDetailsContainer,
                episodePlayerFragment, TAG_PLAYER_FRAGMENT).addToBackStack("player fragment").commit()
            binding.podcastRecyclerView.visibility = View.INVISIBLE
            searchItem?.isVisible = false

        println(" fragment number at ht ${supportFragmentManager.backStackEntryCount}")
    }


}
