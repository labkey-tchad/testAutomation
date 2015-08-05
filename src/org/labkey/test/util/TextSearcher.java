/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;

import java.util.function.Function;
import java.util.function.Supplier;

public class TextSearcher
{
    private Function<String, String> sourceTransformer;
    private Function<String, String> searchTransformer;
    private Supplier<String> sourceGetter;

    private TextSearcher()
    {
        this.sourceTransformer = TextTransformers.IDENTITY;
        this.searchTransformer = TextTransformers.ENCODER;
    }

    public TextSearcher(final Supplier<String> sourceGetter)
    {
        this();
        this.sourceGetter = sourceGetter;
    }

    public TextSearcher(final BaseWebDriverTest test)
    {
        this(test::getHtmlSource);
    }

    public final TextSearcher setSourceTransformer(Function<String, String> sourceTransformer)
    {
        this.sourceTransformer = sourceTransformer;
        return this;
    }

    public final TextSearcher setSearchTransformer(Function<String, String> searchTransformer)
    {
        this.searchTransformer = searchTransformer;
        return this;
    }

    public final TextSearcher setSourceGetter(Supplier<String> sourceGetter)
    {
        this.sourceGetter = sourceGetter;
        return this;
    }

    public final void searchForTexts(TextHandler textHandler, String[] texts)
    {
        if (null == texts || 0 == texts.length)
            return;

        String transformedSource = sourceTransformer.apply(sourceGetter.get());

        for (String text : texts)
        {
            String transformedText = searchTransformer.apply(text);
            if (!textHandler.handle(transformedSource, transformedText))
                return;
        }
    }

    public interface TextHandler
    {
        // Return true to continue searching
        boolean handle(String htmlSource, String text);
    }

    public static abstract class TextTransformers
    {
        public static final Function<String, String> ENCODER = BaseWebDriverTest::encodeText;
        public static final Function<String, String> IDENTITY = text -> text;
    }
}
