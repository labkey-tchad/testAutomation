/*
 * Copyright (c) 2010-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.util.LinkedList;
import java.util.List;

/**
 * User: Trey Chadick
 * Date: Apr 30, 2010
 * Time: 7:53:50 AM
 */
public class SearchHelper extends AbstractHelper
{
    private static LinkedList<SearchItem> _searchQueue = new LinkedList<SearchItem>();

    public SearchHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    public void initialize()
    {
        _searchQueue.clear();
        deleteIndex();
    }
        
    public void verifySearchResults(String container, boolean crawlResults)
    {
        _test.log("Verify search results.");

        List<SearchItem> notFound = verifySearchItems(_searchQueue, container, crawlResults);

        if (!notFound.isEmpty())
        {
            _test.sleep(5000);
            notFound = verifySearchItems(notFound, container, crawlResults);

            if (!notFound.isEmpty())
            {
                _test.sleep(10000);
                notFound = verifySearchItems(notFound, container, crawlResults);

                Assert.assertTrue("These items were not found: " + notFound.toString(), notFound.isEmpty());
            }
        }
    }

    private List<SearchItem> verifySearchItems(List<SearchItem> items, String container, boolean crawlResults)
    {
        _test.log("Verifying " + items.size() + " items");
        LinkedList<SearchItem> notFound = new LinkedList<SearchItem>();

        for ( SearchItem item : items)
        {
            searchFor(item._searchTerm);

            if(item._searchResults.length == 0)
            {
                _test.assertTextPresent("Found 0 results");
            }
            else
            {
                boolean success = true;

                for( Locator loc : item._searchResults )
                {
                    if (!_test.isElementPresent(loc))
                    {
                        success = false;
                        break;
                    }
                }

                if (!success)
                {
                    notFound.add(item);
                    continue;
                }

                if ( container != null )
                {
                    if ( _test.isLinkPresentContainingText("@files") )
                        if(container.contains("@files"))
                            _test.assertLinkPresentWithText(container);
                        else
                            _test.assertLinkPresentWithText(container + (item._file ? "/@files" : ""));
                    else
                        _test.assertLinkPresentWithText(container);
                }

                if ( crawlResults )
                {
                    Assert.fail("Search result crawling not yet implemented");
                }
            }
        }

        if (notFound.isEmpty())
            _test.log("All items were found");
        else
            _test.log(notFound.size() + " items were not found.");

        return notFound;
    }

    public void verifyNoSearchResults()
    {
        _test.log("Verify null search results.");
        for( SearchItem item : _searchQueue )
        {
            searchFor(item._searchTerm);
            for ( Locator loc : item._searchResults )
            {
                _test.assertElementNotPresent(loc);
            }
        }
    }

    public void enqueueSearchItem(String searchTerm, Locator... expectedResults)
    {
        _searchQueue.add(new SearchItem(searchTerm, false, expectedResults));
    }

    public void enqueueSearchItem(String searchTerm, boolean file, Locator... expectedResults)
    {
        _searchQueue.add(new SearchItem(searchTerm, file, expectedResults));
    }

    public void searchFor(String searchTerm)
    {
        _test.log("Searching for: '" + searchTerm + "'.");
        if ( _test.isElementPresent(Locator.id("query")) )
        {
            _test.setFormElement(Locator.id("query"), searchTerm);
            _test.clickButton("Search");
        }
        else
        {
            _test.setFormElement(Locator.id("headerSearchInput-inputEl"), searchTerm);
            _test.clickAndWait(Locator.xpath("//img[@src = '/labkey/_images/search.png']"));
        }
    }

    public void searchForSubjects(String searchTerm)
    {
        _test.log("Searching for subject: '" + searchTerm + "'.");
        if (!_test.isElementPresent(Locator.id("query")) )
            _test.goToModule("Search");
        if (_test.getAttribute(Locator.id("adv-search-btn"), "src").contains("plus"))
            _test.click(Locator.id("adv-search-btn"));

        _test.checkCheckbox("category", 2);

        _test.setFormElement(Locator.id("query"), searchTerm);
        _test.clickButton("Search");
    }

    public void deleteIndex()
    {
        _test.ensureAdminMode();
        _test.goToAdmin();
        _test.clickAndWait(Locator.linkWithText("full-text search"));
        _test.clickButton("Delete Index");
    }

    public static class SearchItem
    {
        public String _searchTerm;
        public Locator[] _searchResults;
        public boolean _file; // is this search expecting a file?

        public SearchItem(String term, boolean file, Locator... results)
        {
            _searchTerm = term;
            _searchResults = results;
            _file = file;
        }

        @Override
        public String toString()
        {
            return _searchTerm;
        }
    }
}
