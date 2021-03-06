/**
 * 
 */
package com.searchmavenapp.android.maven.search;

import com.searchmavenapp.android.maven.search.activities.ArtifactDetails;
import com.searchmavenapp.android.maven.search.activities.Main;
import com.searchmavenapp.android.maven.search.activities.MainAdvancedSearch;
import com.searchmavenapp.android.maven.search.activities.SearchResults;
import com.searchmavenapp.android.maven.search.constants.Constants;
import com.searchmavenapp.android.maven.search.restletapi.MavenCentralRestAPI;
import com.searchmavenapp.android.maven.search.restletapi.dao.MCRDoc;
import com.searchmavenapp.android.maven.search.restletapi.dao.MCRResponse;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

/**
 * @author Michael
 * 
 */
public class ProgressThread extends Thread
{
  private Handler  iHandler;
  private Activity iActivity;
  private int      iSearchType;

  // TODO: convert pSearchType to an ENUM
  public ProgressThread(Activity pActivity, int pSearchType)
  {
    iHandler = new SearchResultsHandler(pActivity);
    iActivity = pActivity;
    iSearchType = pSearchType;
  }

  /**
   * Perform the search in a separate thread by calling the MavenCentralRestAPI Class to get back SearchResults
   * Create a message to send back to the Handler. The message's arg1 is the search type code, arg2 is the code to indicate
   * the destination screen (if other than the search results which is the default), and obj is the search results object
   */
  public void run()
  {
    Message msg = iHandler.obtainMessage();
    Object searchResults = null;
    MavenCentralRestAPI mcr = new MavenCentralRestAPI(iActivity);

    switch (iSearchType)
    {
      case Constants.PROGRESS_DIALOG_QUICK_SEARCH:
      {
        searchResults = handleQuickSearch(mcr);
        break;
      }
      case Constants.PROGRESS_DIALOG_ADVANCED_SEARCH:
      {
        searchResults = handleAdvancedSearch(mcr);
        break;
      }
      case Constants.PROGRESS_DIALOG_ARTIFACT_DETAILS:
      {
        SearchResults rsr = (SearchResults) iActivity;

        msg.arg2 = Constants.PROGRESS_DIALOG_ARTIFACT_DETAILS;
        searchResults = new MCRDoc();
        ((MCRDoc)searchResults).setG(rsr.getSelectedGroup());
        ((MCRDoc)searchResults).setA(rsr.getSelectedArtifact());
        ((MCRDoc)searchResults).setV(rsr.getSelectedVersion());
        break;
      }
      case Constants.PROGRESS_DIALOG_GROUPID_SEARCH:
      {
        searchResults = handleGroupIdSearch(mcr);
        break;
      }
      case Constants.PROGRESS_DIALOG_ARTIFACTID_SEARCH:
      {
        searchResults = handleArtifactIdSearch(mcr);
        break;
      }
      case Constants.PROGRESS_DIALOG_VERSION_SEARCH:
      {
        SearchResults rsr = (SearchResults) iActivity;

        Integer selectedVersionCount = rsr.getSelectedVersionCount();

        if (selectedVersionCount > 1)
        {
          searchResults = handleVersionSearch(mcr, rsr);
        }
        else
        {
          msg.arg2 = Constants.PROGRESS_DIALOG_ARTIFACT_DETAILS;
          searchResults = new MCRDoc();
          ((MCRDoc)searchResults).setG(rsr.getSelectedGroup());
          ((MCRDoc)searchResults).setA(rsr.getSelectedArtifact());
          ((MCRDoc)searchResults).setV(rsr.getSelectedVersion());
        }
        break;
      }
      case Constants.PROGRESS_DIALOG_POM_VIEW:
      {
        ArtifactDetails rsr = (ArtifactDetails) iActivity;

        msg.arg2 = Constants.PROGRESS_DIALOG_POM_VIEW;
        searchResults = mcr.downloadFile(rsr.getGTVText(), rsr.getATVText(), rsr.getVTVText());
        break;
      }
      default:
        // TODO: bad search type, throw runtime exception, i think?
        break;
    }

    msg.arg1 = iSearchType;
    msg.obj = searchResults;

    iHandler.sendMessage(msg);
  }

  private MCRResponse handleQuickSearch(MavenCentralRestAPI pMcr)
  {
    Main m = (Main) iActivity;
    EditText et = m.getSearchEditText();

    MCRResponse searchResults = pMcr.searchBasic(et.getText().toString().trim());
    return searchResults;
  }

  private MCRResponse handleAdvancedSearch(MavenCentralRestAPI pMcr)
  {
    MainAdvancedSearch mas = (MainAdvancedSearch) iActivity;

    String groupId = mas.getGroupIdSearchText().getText().toString().trim();
    String artifactId = mas.getArtifactIdSearchText().getText().toString().trim();
    String version = mas.getVersionSearchText().getText().toString().trim();
    String packaging = mas.getPackagingSearchText().getText().toString().trim();
    String classifier = mas.getClassifierSearchText().getText().toString().trim();
    String className = mas.getClassNameSearchText().getText().toString().trim();

    MCRResponse searchResults = null;
    if ("Search".equals(className) || className.trim().length() == 0)
    {
      searchResults = pMcr.searchCoordinate(groupId, artifactId, version, packaging, classifier);
    }
    else
    {
      searchResults = pMcr.searchClassName(className);
    }
    return searchResults;
  }

  private MCRResponse handleGroupIdSearch(MavenCentralRestAPI pMcr)
  {
    SearchResults rsr = (SearchResults) iActivity;
    String selectedGroupId = rsr.getSelectedGroup();

    MCRResponse searchResults = pMcr.searchForGroupId(selectedGroupId);
    return searchResults;
  }

  private MCRResponse handleArtifactIdSearch(MavenCentralRestAPI pMcr)
  {
    SearchResults rsr = (SearchResults) iActivity;
    String selectedArtifactId = rsr.getSelectedArtifact();

    MCRResponse searchResults = pMcr.searchForArtifactId(selectedArtifactId);
    return searchResults;
  }

  private MCRResponse handleVersionSearch(MavenCentralRestAPI pMcr, SearchResults pRsr)
  {
    String selectedGroupId = pRsr.getSelectedGroup();
    String selectedArtifactId = pRsr.getSelectedArtifact();

    MCRResponse searchResults = pMcr.searchForAllVersions(selectedGroupId, selectedArtifactId);
    return searchResults;
  }

}
