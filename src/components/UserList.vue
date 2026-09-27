<template>
    <v-app-bar height="40"    
        class="mt-5"
        color="purple-lighten-5"
        dense
        dark
        flat
    >


    <v-spacer></v-spacer>
    <v-btn tile @click="newUser()" target="_blank" text>
      New User
      <v-icon>mdi-account</v-icon>
    </v-btn>    
    <v-btn tile @click="fetchData()" target="_blank" text>
      Refresh
      <v-icon>mdi-database-refresh</v-icon>
    </v-btn>    
  </v-app-bar>
    
  <v-dialog max-width="700px" v-model="user.dialog" persistent>
    <v-card class="pa-3">
      <v-card-title class="pa-4" style="background-color: purple; color: white; font-size: large; font-weight: bold;">User</v-card-title>
      <v-card-text>
        <v-form dense>
          
          <v-row class="px-0 mx-0">
            <v-col cols=2 class="pt-4 pb-0 px-0 ma-0"><div>SID</div></v-col>
            <v-col cols=5 class="pa-0 ma-0"><v-text-field v-model="currentUser.id"></v-text-field></v-col>
          </v-row>
          <v-row class="pa-0 ma-0">
            <v-col cols=2 class="pt-4 pb-0 px-0 ma-0"><div>DN</div></v-col>
            <v-col cols=10 class="pa-0 ma-0"><v-text-field v-model="currentUser.dn"></v-text-field></v-col>
          </v-row>
          <v-row class="pa-0 ma-0">
            <v-col cols=2 class="pt-4 pb-0 px-0 ma-0"><div>Role</div></v-col>
            <v-col cols=10 class="pa-0 ma-0"> <v-select
                                                    :items="roles"
                                                    density="compact"
                                                    label="Role"
                                                    v-model="currentUser.role"
                                              ></v-select></v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-btn color="blue-darken-1" text @click="user.dialog=false;saveUser();">Save</v-btn>
        <v-btn color="blue-darken-1" text @click="user.dialog=false;">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>      


  <EasyDataTable
    :headers="headers"
    :items="users"
    :loading="loading"
    
  >
    <template #item-actions="item">
        <v-container><v-row justify="end">
        <v-col cols=6>
        <v-btn icon dense size="small" @click="editUser(item);"><v-icon>mdi-pencil</v-icon></v-btn>
        </v-col>
        <v-col cols=6>
        <v-btn icon dense size="small" @click="deleteUser(item.id);"><v-icon>mdi-delete</v-icon></v-btn>
        </v-col>
        </v-row></v-container>
    </template>
  </EasyDataTable>
</template>

<script>
    import axios from "axios";

    export default {
        emits: ['alerts','alert','status','token','user','role'],
        props: {
          cortana_token: String,
          cortana_user: String,
          cortana_role: String,
        },
        data() {
            return {
                roles: ['user','admin'],
                currentUser: {
                    id: null,
                    role: null,
                    dn: null,
                    active: true,
                },
                user: {
                    dialog: false,
                },                
                users: [],
                headers: [
                    {
                    key: "id",
                    text: "SID",
                    align: "left",
                    sortable: true,
                    value: "id",
                    filterable: true,
                    width:25 
                    },
                    {
                    key: "role",
                    text: "Role",
                    align: "left",
                    sortable: true,
                    value: "role",
                    filterable: true,
                    width:25
                    },
                    {
                    key: "dn",
                    text: "DN",
                    align: "left",
                    sortable: true,
                    value: "dn",
                    filterable: true,
                    width:200
                    },
                    {
                    key: "actions",
                    text: "",
                    align: "start",
                    sortable: false,
                    value: "actions",
                    width: 50,
                    filterable: false
                    }           
                ],
                loading: true,
                
            }
        },
        watch: {
          cortana_token: {
            handler() {
              console.log("cortana_token set on user list: "+this.cortana_token);
            }
          },
        },
        mounted() {
            setTimeout(() => {
            console.log("*** USERS TIMEOUT FIRED ***");
            
            this.fetchData();
            }, 1000);

        },
        methods: {
            handleError(error) {
              this.$emit("alert", error, "error");

            },
            fetchData() {
                this.loading = true;
                let urlString = this.baseUrl;
                urlString = urlString + "users";

                    console.log("FETCHING: "+urlString)
                    axios.get(urlString, {headers: {"CORTANA-TOKEN": this.cortana_token}})
                    .then(response => {
                        console.log(response.data.payload);
                        this.users = response.data.payload.users;
                        })
                    .catch(error => {
                        console.log(error);
                        this.handleError(error);
                        this.loading = false;
                    });

                this.loading = false;
            },
            newUser() {
                this.currentUser.id = null;
                this.currentUser.dn = null;
                this.currentUser.active = true;
                this.currentUser.role = "user";
                this.user.dialog = true;
    
            },
            deleteUser(id) {
                axios.delete(this.baseUrl + "user?id="+id,{headers: {"CORTANA-TOKEN": this.cortana_token}})
                //eslint-disable-next-line no-unused-vars
                .then(response => {
                    this.fetchData();
                })
                .catch(error => {
                    this.handleError(error);
                });                
            },
            saveUser() {
                axios.post(this.baseUrl + "user", this.currentUser,{headers: {"CORTANA-TOKEN": this.cortana_token}})
                //eslint-disable-next-line no-unused-vars
                .then(response => {
                    this.fetchData();
                    //this.onSuccess(response);
                })
                .catch(error => {
                this.handleError(error);
                });
            },
            editUser(item) {
                this.currentUser.id = item.id;
                this.currentUser.dn = item.dn;
                this.currentUser.active = item.active;
                this.currentUser.role = item.role;                                                                     
                this.user.dialog = true;
            }
        }
    }
</script>