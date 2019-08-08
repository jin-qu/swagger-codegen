/**
 * Swagger Petstore
 * This is a sample server Petstore server.  You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you can use the api key `special-key` to test the authorization filters.
 *
 * OpenAPI spec version: 1.0.0
 * Contact: apiteam@swagger.io
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

import { IAjaxProvider } from 'jinqu';
import { LinqService, QueryOptions } from 'linquest';
import {
  Pet,
  ApiResponse,
} from './models';

/**
 * PetApi - API class
 */
export class PetApi extends LinqService {

  /**
   * Creates a new PetApi class.
   *
   * @param ajaxProvider The Linquest Ajax Provider to inject. When not specified, default Fetch provider will be used.
   */
  constructor(ajaxProvider?: IAjaxProvider) {
    super('http://petstore.swagger.io/v2', ajaxProvider);
  }

  /**
   * Add a new pet to the store
   * 
   * @param body Pet object that needs to be added to the store
   */
  addPet(body: Pet, options?: QueryOptions) {
    // create query
    const url = `/pet`;
    let query = this.createQuery<any>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'POST';
    options.params = options.params || [];
    options.headers = options.headers || {};

    // set body parameter
    options.data = body;
    query = query.withOptions(options);

    return query.firstAsync();
  }

  /**
   * Deletes a pet
   * 
   * @param petId Pet id to delete
   * @param apiKey 
   */
  deletePet(petId: number, apiKey?: string, options?: QueryOptions) {
    // create query
    const url = `/pet/{petId}`
        .replace('{petId}', encodeURIComponent(petId.toString()));
    let query = this.createQuery<any>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'DELETE';
    options.params = options.params || [];
    options.headers = options.headers || {};

    // set headers
    options.headers['api_key'] = apiKey && apiKey.toString();
    query = query.withOptions(options);

    return query.firstAsync();
  }

  /**
   * Finds Pets by status
   * Multiple status values can be provided with comma separated strings
   * @param status Status values that need to be considered for filter
   */
  findPetsByStatus(status: Array<'available' | 'pending' | 'sold'>, options?: QueryOptions) {
    // create query
    const url = `/pet/findByStatus`;
    let query = this.createQuery<Pet>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'GET';
    options.params = options.params || [];
    options.headers = options.headers || {};

    // set query parameters
    options.params.push({ key: 'status', value: status && status.toString() });
    query = query.withOptions(options);

    return query;
  }

  /**
   * Finds Pets by tags
   * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
   * @param tags Tags to filter by
   */
  findPetsByTags(tags: Array<string>, options?: QueryOptions) {
    // create query
    const url = `/pet/findByTags`;
    let query = this.createQuery<Pet>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'GET';
    options.params = options.params || [];
    options.headers = options.headers || {};

    // set query parameters
    options.params.push({ key: 'tags', value: tags && tags.toString() });
    query = query.withOptions(options);

    return query;
  }

  /**
   * Find pet by ID
   * Returns a single pet
   * @param petId ID of pet to return
   */
  getPetById(petId: number, options?: QueryOptions) {
    // create query
    const url = `/pet/{petId}`
        .replace('{petId}', encodeURIComponent(petId.toString()));
    let query = this.createQuery<Pet>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'GET';
    options.params = options.params || [];
    options.headers = options.headers || {};

    query = query.withOptions(options);

    return query.firstAsync();
  }

  /**
   * Update an existing pet
   * 
   * @param body Pet object that needs to be added to the store
   */
  updatePet(body: Pet, options?: QueryOptions) {
    // create query
    const url = `/pet`;
    let query = this.createQuery<any>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'PUT';
    options.params = options.params || [];
    options.headers = options.headers || {};

    // set body parameter
    options.data = body;
    query = query.withOptions(options);

    return query.firstAsync();
  }

  /**
   * Updates a pet in the store with form data
   * 
   * @param petId ID of pet that needs to be updated
   * @param name Updated name of the pet
   * @param status Updated status of the pet
   */
  updatePetWithForm(petId: number, name?: string, status?: string, options?: QueryOptions) {
    // create query
    const url = `/pet/{petId}`
        .replace('{petId}', encodeURIComponent(petId.toString()));
    let query = this.createQuery<any>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'POST';
    options.params = options.params || [];
    options.headers = options.headers || {};

    // set form parameters
    options.data = options.data || {};
    options.data['name'] = name;
    options.data['status'] = status;
    query = query.withOptions(options);

    return query.firstAsync();
  }

  /**
   * uploads an image
   * 
   * @param petId ID of pet to update
   * @param additionalMetadata Additional data to pass to server
   * @param file file to upload
   */
  uploadFile(petId: number, additionalMetadata?: string, file?: any, options?: QueryOptions) {
    // create query
    const url = `/pet/{petId}/uploadImage`
        .replace('{petId}', encodeURIComponent(petId.toString()));
    let query = this.createQuery<ApiResponse>(url);

    // initialize options
    options = options || {};
    options.method = options.method || 'POST';
    options.params = options.params || [];
    options.headers = options.headers || {};

    // set form parameters
    options.data = options.data || {};
    options.data['additionalMetadata'] = additionalMetadata;
    options.data['file'] = file;
    query = query.withOptions(options);

    return query.firstAsync();
  }
}