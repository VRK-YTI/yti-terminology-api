package fi.vm.yti.terminology.elasticsearch.query;

import fi.vm.yti.terminology.api.util.ElasticRequestUtils;
import org.elasticsearch.index.query.QueryStringQueryBuilder;;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElasticRequestUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"+", "-", "=", "&", "|", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "\\", "/"})
    void testBuildSpecialCharacters(String prefix){
        QueryStringQueryBuilder request = ElasticRequestUtils.buildPrefixSuffixQuery(prefix);
        // \prefix \prefix* *\prefix
        assertEquals("\\" + prefix + " \\" + prefix + "* *\\" + prefix, request.queryString());
    }


}
