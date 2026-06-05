package api.dao.comparison;

import models.BaseModel;
import org.assertj.core.api.AbstractAssert;

import static jdk.dynalink.linker.support.Guards.isNotNull;

public class DaoAndModelAssertions {

    private static final DaoComparator daoComparator = new DaoComparator();

    public static DaoModelAssert assertThat(BaseModel apiModel, Object daoModel) {
        return new DaoModelAssert(apiModel, daoModel);
    }

    public static class DaoModelAssert extends AbstractAssert<DaoModelAssert, BaseModel> {

        private final Object daoModel;

        public DaoModelAssert(BaseModel apiModel, Object daoModel) {
            super(apiModel, DaoModelAssert.class);
            this.daoModel = daoModel;
        }

        public DaoModelAssert matches() {
            // Проверяем, что API модель не null
            isNotNull();

            // Проверяем, что DAO модель не null
            if (daoModel == null) {
                failWithMessage("DAO model should not be null");
            }

            // Use configurable comparison
            try {
                daoComparator.compare(actual, daoModel);
            } catch (AssertionError e) {
                failWithMessage(e.getMessage());
            }

            return this;
        }

        // Альтернативный вариант без использования failWithMessage
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