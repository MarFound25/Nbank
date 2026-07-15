package requests.skelethon.requesters;

import requests.skelethon.interfaces.GetAllEndpointInterface;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

import java.util.Arrays;
import java.util.List;

public class ValidatedCrudRequester<T extends BaseModel> extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
    private CrudRequesters crudRequester;
    private String endpointUrl;
    private ResponseSpecification responseSpec;

    public ValidatedCrudRequester(RequestSpecification requestSpecification, String endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification);
        this.endpointUrl = endpoint;
        this.responseSpec = responseSpecification;
        this.crudRequester = new CrudRequesters(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public T post(BaseModel model) {
        return (T) crudRequester.post(model);
    }

    @Override
    public T get(long id) {
        return (T) crudRequester.get(id);
    }

    @Override
    public Object update(long id, BaseModel model) {
        return crudRequester.update(id, model);
    }

    @Override
    public Object delete(long id) {
        return crudRequester.delete(id);
    }

    @Override
    public List<T> getAll(Class<?> clazz) {
        T[] array = (T[]) crudRequester.getAll(clazz);
        return Arrays.asList(array);
    }
}
