package pt.isec.ans.tp2_amov

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    val SERVER_PORT: Int = 9999
    var nome: String = ""
    var nplayers: Int = 0
    var idPlayer: String = ""
    private val fineLocation = 101

    private var socket: Socket? = null
    private val socketI: InputStream?
        get() = socket?.getInputStream()
    private val socketO: OutputStream?
        get() = socket?.getOutputStream()

    var serverSocket: ServerSocket? = null
    var strIpAdress: String = ""

    var db = FirebaseFirestore.getInstance()
    var dlg: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        verificaPermissoes(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            "localizacao",
            fineLocation
        )

        var auxCon: Boolean = isInternetAvailable()
        if (!auxCon) {
            thread {
                while (true) {
                    auxCon = isInternetAvailable()
                }
            }
            esperaNet()
        }

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        strIpAdress = String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )
        println(strIpAdress)

        findViewById<Button>(R.id.btnServer).setOnClickListener {
            startServerMode();
        }
        findViewById<Button>(R.id.btnClient).setOnClickListener {
            clientMode()
        }
    }

    fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    private fun esperaNet() {
        val ll = LinearLayout(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.setPadding(50, 50, 50, 50)
            layoutParams = params
            setBackgroundColor(Color.rgb(240, 224, 208))
            orientation = LinearLayout.HORIZONTAL
            addView(ProgressBar(context).apply {
                isIndeterminate = true
                val paramsPB = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                paramsPB.gravity = Gravity.CENTER_VERTICAL
                layoutParams = paramsPB
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(96, 96, 32))
            })
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = paramsTV
                text = "Esperando ligacao a internet"
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
        }

        dlg = AlertDialog.Builder(this).run {
            setTitle("Internet")
            setView(ll)
            setOnCancelListener {
                finish()
            }
            create()
        }
        dlg?.show()
    }

    private fun verificaPermissoes(permissao: String, nome: String, code: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    permissao
                ) == PackageManager.PERMISSION_DENIED -> {
                    ActivityCompat.requestPermissions(this, arrayOf(permissao), code)
                }
            }
        }
    }

    fun startServerMode() {
        val edNomeEquipa = EditText(this).apply {
            maxLines = 1
        }
        val edNumJogadores = EditText(this).apply {
            maxLines = 1
        }
        var ll = LinearLayout(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.setPadding(50, 50, 50, 50)
            layoutParams = params
            setBackgroundColor(Color.rgb(0, 150, 150))
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = paramsTV
                text = String.format("Nome da equipa: ")
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
            addView(edNomeEquipa)
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = paramsTV
                text = String.format("Numero de elementos: ")
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
            addView(edNumJogadores)
        }

        val dlg = AlertDialog.Builder(this).run {
            setTitle("Jogo")
            setPositiveButton("Ligar") { _: DialogInterface, _: Int ->
                nome = edNomeEquipa.text.toString()
                nplayers = edNumJogadores.text.toString().toInt()
                if (nome.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Erro", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    var docData = hashMapOf("flag" to false)
                    db.collection("Equipas").document("${nome}").set(docData)
                    var i = 1
                    while (i <= nplayers) {
                        var d = hashMapOf("p${i}" to arrayListOf(0, 0))
                        db.collection("Equipas").document("${nome}").set(
                            d,
                            SetOptions.merge()
                        )
                        i++
                    }
                    conectServer()
                }
            }
            setNegativeButton("Voltar") { _: DialogInterface, _: Int ->
                finish()
            }
            setCancelable(false)
            setView(ll)
            create()
        }
        dlg.show()
    }

    fun conectServer() {
        val ll = LinearLayout(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.setPadding(50, 50, 50, 50)
            layoutParams = params
            setBackgroundColor(Color.rgb(240, 224, 208))
            orientation = LinearLayout.HORIZONTAL
            addView(ProgressBar(context).apply {
                isIndeterminate = true
                val paramsPB = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                paramsPB.gravity = Gravity.CENTER_VERTICAL
                layoutParams = paramsPB
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(96, 96, 32))
            })
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = paramsTV
                text = "IP: ${strIpAdress}"
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
        }

        var dlg = AlertDialog.Builder(this).run {
            setTitle("Title")
            setView(ll)
            setOnCancelListener {
                serverSocket?.close()
                serverSocket = null
                finish()
            }
            create()
        }
        if (serverSocket != null || socket != null)
            return

        thread {
            var i = 1
            serverSocket = ServerSocket(SERVER_PORT)
            do {
                var menssagem: Mensagem = Mensagem(nome, "p${i + 1}", nplayers)
                var strMenssagem: String = Gson().toJson(menssagem)
                serverSend(serverSocket!!.accept(), strMenssagem)
                i++
            } while (i < nplayers)


            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("NOME_EQUIPA", nome)
            intent.putExtra("NJOGADORES", nplayers)
            intent.putExtra("IDJOGADOR", "p1")
            startActivity(intent)
        }
        dlg.show()
    }

    fun serverSend(newSocket: Socket, menssagem: String) {
        socket = newSocket
        try {
            socketO?.run {
                try {
                    val printStream = PrintStream(this)
                    printStream.println(menssagem)
                    printStream.flush()
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
            serverSocket?.close()
            serverSocket = null
        }
    }

    fun clientMode() {
        val edtBox = EditText(this).apply {
            maxLines = 1
            filters = arrayOf(object : InputFilter {
                override fun filter(
                    source: CharSequence?,
                    start: Int,
                    end: Int,
                    dest: Spanned?,
                    dstart: Int,
                    dend: Int
                ): CharSequence? {
                    if (source?.none { it.isDigit() || it == '.' } == true)
                        return ""
                    return null
                }
            })
        }
        val dlg = AlertDialog.Builder(this).run {
            setTitle("Ligar")
            setMessage("Introduzir IP:")
            setPositiveButton("Connect") { _: DialogInterface, _: Int ->
                val strIP = edtBox.text.toString()
                if (strIP.isEmpty() || !Patterns.IP_ADDRESS.matcher(strIP).matches()) {
                    Toast.makeText(this@MainActivity, "Erro", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    conectClient(strIP)
                }
            }
            setNegativeButton("Cancelar") { _: DialogInterface, _: Int ->
                finish()
            }
            setCancelable(false)
            setView(edtBox)
            create()
        }
        dlg.show()
    }

    fun conectClient(ip: String) {
        if (socket != null)
            return
        thread {
            try {
                socket = Socket(ip, SERVER_PORT)
                val bufI = socketI!!.bufferedReader()
                var strMen = bufI.readLine()
                var mens = Gson().fromJson(strMen, Mensagem::class.java)
                nome = mens.nome
                println(nome)
                nplayers = mens.nPlayers
                idPlayer = mens.idPlayer
            } catch (_: Exception) {
                finish()
            }
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("NOME_EQUIPA", nome)
            intent.putExtra("NJOGADORES", nplayers)
            intent.putExtra("IDJOGADOR", idPlayer)
            startActivity(intent)
        }
    }
}