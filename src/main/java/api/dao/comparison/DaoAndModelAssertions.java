package api.dao.comparison;

import org.assertj.core.api.AbstractAssert;

public class DaoAndModelAssertions {

    private static final DaoComparator daoComparator = new DaoComparator();

    public static DaoModelAssert assertThat(Object apiModel, Object daoModel) {
        return new DaoModelAssert(apiModel, daoModel);
    }

    public static class DaoModelAssert extends AbstractAssert<DaoModelAssert, Object> {

        private final Object daoModel;

        public DaoModelAssert(Object apiModel, Object daoModel) {
            super(apiModel, DaoModelAssert.class);
            this.daoModel = daoModel;
        }

        public DaoModelAssert matches() {
            isNotNull();

            if (daoModel == null) {
                failWithMessage("DAO model should not be null");
            }

            try {
                daoComparator.compare(actual, daoModel);
            } catch (AssertionError e) {
                failWithMessage(e.getMessage());
            }

            return this;
        }

        public DaoModelAssert matchesAlternative() {
            isNotNull();

            org.assertj.core.api.Assertions.assertThat(daoModel)
                    .as("DAO model should not be null")
                    .isNotNull();

            try {
                daoComparator.compare(actual, daoModel);
            } catch (AssertionError e) {
                org.assertj.core.api.Assertions.fail(e.getMessage());
            }

            return this;
        }
    }
}