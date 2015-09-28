package com.example.guto.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceList extends Activity{

    // String de retorno para intent
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //  Componentes do layout
    Button scanButton;
    ListView pairedListView;
    ListView newDevicesListView;

    // Adaptador Bluetooth e vetores para dispositivos pareados e novos dispositivos
    private BluetoothAdapter BtAdapter;
    private ArrayAdapter<String> PairedDevices;
    private ArrayAdapter<String> NewDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_list);

        // Caso usuário clique em return
        setResult(Activity.RESULT_CANCELED);

        // Pega referencia para os componentes do layout
        pairedListView = (ListView) findViewById(R.id.paired_devices);
        newDevicesListView = (ListView) findViewById(R.id.new_devices);
        scanButton = (Button) findViewById(R.id.button_scan);

        // Inicializa adapters das ListViews
        PairedDevices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        NewDevices = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);

        // Define adapters das listViews
        pairedListView.setAdapter(PairedDevices);
        newDevicesListView.setAdapter(NewDevices);


        // Listeners dos componentes:
        // Clique botao scan
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Ativa procura por dispositivos
                if (BtAdapter.isDiscovering()) {
                    BtAdapter.cancelDiscovery();
                }
                BtAdapter.startDiscovery();
                // Desabilita botão
                v.setVisibility(View.GONE);
            }
        });
        // Listeners para clique em um item das listas:
        pairedListView.setOnItemClickListener(DeviceClickListener);
        newDevicesListView.setOnItemClickListener(DeviceClickListener);


        // Registra filtros para o broadcast receiver:
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);   // Dispositivo encontrado
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);  // Registro do fim da descoberta de dispositivos
        this.registerReceiver(mReceiver, filter);



        // BtAdapter refere-se ao adaptador Bluetooth do dispositivo atual
        BtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Recebe a lista dos dispositivos pareados
        Set<BluetoothDevice> pairedDevices = BtAdapter.getBondedDevices();

        // Adiciona cada um deles ao vetor PairedDevices
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                PairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "None paired";
            PairedDevices.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Termina o processo de descoberta de dispositivos
        if (BtAdapter != null) {
            BtAdapter.cancelDiscovery();
        }
        // Libera listeners de broadcast
        this.unregisterReceiver(mReceiver);
    }



    // Listener para os itens da lista de dispositivos
    private OnItemClickListener DeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            // Como já selecionei um dispositivo, posso cancelar a procura por novos (não desativar pode deixar a conexão lenta)
            BtAdapter.cancelDiscovery();

            // Obtem item selecionado
            String info = ((TextView) v).getText().toString();
            // Endereço do dispositivo consiste nos últimos 17 caracteres exibidos na lista
            String address = info.substring(info.length() - 17);

            // Cria o resultado do intent e passa o MAC como parametro
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Define resultado e termina activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };


    // Broadcast Receiver para receber novos dispositivos encontrados:
    // Será ativado qndo encontrar novo dispositivo ou terminar a busca, conforme registrado em onCreate()
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        // Ao ser ativado
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();


            if (BluetoothDevice.ACTION_FOUND.equals(action)) {       // Quando  um dispositivo é encontrado

                // Recupera o dispositivo do intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Se não estiver pareado, adiciona a lista de new devices
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    NewDevices.add(device.getName() + "\n" + device.getAddress());
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {      // Fim da descoberta
                if (NewDevices.getCount() == 0) {
                    String noDevices = "none paired";
                    NewDevices.add(noDevices);
                }
                scanButton.setVisibility(View.VISIBLE);
            }
        }
    };


}
