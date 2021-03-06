package principal;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alex.sharepdf.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.math.BigInteger;
import java.security.SecureRandom;

import auth.Login;

/**
 * Created by Alex on 12/11/2017.
 */

public class MiCuenta  extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    private int CAMERA_REQUEST_CODE = 0;
    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    Button mi_cuenta_volver, mi_cuenta_cambio_password;
    TextView mi_cuenta_usuario, mi_cuenta_frase_del_dia, mi_cuenta_credito_disponible,
            mi_cuenta_archivos_descargados;
    ImageView mi_cuenta_foto;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.layout_mi_cuenta);

        mi_cuenta_volver = (Button)findViewById(R.id.mi_cuenta_volver);
        mi_cuenta_cambio_password = (Button)findViewById(R.id.mi_cuenta_cambio_password);
        mi_cuenta_usuario = (TextView)findViewById(R.id.mi_cuenta_usuario);
        mi_cuenta_frase_del_dia = (TextView)findViewById(R.id.mi_cuenta_frase_del_dia);
        mi_cuenta_credito_disponible = (TextView)findViewById(R.id.mi_cuenta_credito_disponible);
        mi_cuenta_archivos_descargados = (TextView)findViewById(R.id.mi_cuenta_archivos_descargados);
        mi_cuenta_foto = (ImageView) findViewById(R.id.mi_cuenta_foto);

        mi_cuenta_volver.setOnClickListener(this);
        mi_cuenta_foto.setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    mStorage = FirebaseStorage.getInstance().getReference();
                    mDatabase = FirebaseDatabase.getInstance().getReference().child("Usuarios");
                    mDatabase.child(firebaseAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            mi_cuenta_usuario.setText(String.valueOf(dataSnapshot.child("usuario").getValue().toString()));
                            String imageUrl = String.valueOf(dataSnapshot.child("foto_url").getValue().toString());
                            if (URLUtil.isValidUrl(imageUrl))
                                Picasso.with(MiCuenta.this).load(Uri.parse(imageUrl)).into(mi_cuenta_foto);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                   startActivity(new Intent(MiCuenta.this, Login.class));


                }
                }
        };
    }


    public String getRandomString() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {

            if(mAuth.getCurrentUser() == null)
                return;

            final Uri uri = data.getData();
            if (uri == null)
                return;
            if (mAuth.getCurrentUser() == null)
            return;

            if(mStorage==null)
                mStorage = FirebaseStorage.getInstance().getReference();
            if(mDatabase==null)
                mDatabase = FirebaseDatabase.getInstance().getReference().child("Usuarios");

            final StorageReference filepath = mStorage.child("Fotos_usuarios").child(getRandomString());/*uri.getLastPathSegment()*/
            final DatabaseReference currentUserDB = mDatabase.child(mAuth.getCurrentUser().getUid());
            currentUserDB.child("foto_url").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String image = dataSnapshot.getValue().toString();

                    if (!image.equals("default") && !image.isEmpty()) {
                        Task<Void> task = FirebaseStorage.getInstance().getReferenceFromUrl(image).delete();
                        task.addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful())
                                    Toast.makeText(MiCuenta.this, "Deleted image succesfully", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(MiCuenta.this, "Deleted image failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    currentUserDB.child("foto_url").removeEventListener(this);

                    filepath.putFile(uri).addOnSuccessListener(MiCuenta.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Uri downloadUri = taskSnapshot.getDownloadUrl();
                            Toast.makeText(MiCuenta.this, "Finished", Toast.LENGTH_SHORT).show();
                            Picasso.with(MiCuenta.this).load(uri).fit().centerCrop().into(mi_cuenta_foto);
                            DatabaseReference currentUserDB = mDatabase.child(mAuth.getCurrentUser().getUid());
                            currentUserDB.child("foto_url").setValue(downloadUri.toString());
                        }
                    }).addOnFailureListener(MiCuenta.this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MiCuenta.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.mi_cuenta_volver:
                Intent principal = new Intent(MiCuenta.this, Principal.class);
                startActivity(principal);
                break;
            case R.id.mi_cuenta_foto:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(Intent.createChooser(intent, "Select a picture for your profile"), CAMERA_REQUEST_CODE);
                }
                break;

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
}
