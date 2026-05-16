package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;

public class ValidatedCrudRequester<T extends BaseModel> {

    private final CrudRequesters crudRequesters;
    private final Class<T> responseType;

    public ValidatedCrudRequester(RequestSpecification requestSpecification,
                                  ResponseSpecification responseSpecification,
                                  Class<T> responseType) {
        this.crudRequesters = new CrudRequesters(requestSpecification, responseSpecification);
        this.responseType = responseType;
    }

    @SuppressWarnings("unchecked")
    public T post(BaseModel model) {
        return (T) crudRequesters.create(model)
                .extract()
                .as(responseType);
    }

    @SuppressWarnings("unchecked")
    public T get(long id) {
        return (T) crudRequesters.readOne(id)
                .extract()
                .as(responseType);
    }

    @SuppressWarnings("unchecked")
    public T update(long id, BaseModel model) {
        return (T) crudRequesters.update(id, model)
                .extract()
                .as(responseType);
    }

    public ValidatableResponse delete(long id) {
        return crudRequesters.delete(id);
    }
}