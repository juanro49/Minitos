package org.juanro.minitos.gui.fragment;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.juanro.minitos.gui.activity.MapActivity;
import org.juanro.minitos.model.entity.VehicleWithFavorite;

import org.juanro.minitos.R;
import org.juanro.minitos.gui.adapter.VehiclesListAdapter;
import org.juanro.minitos.viewmodel.VehiclesViewModel;

public class VehiclesListFragment extends Fragment implements ViewModelStoreOwner {
    private static final String KEY_VEHICLE = "vehicle";
    private static final String KEY_EMPTY_LIST_TEXT = "empty_list_text_key";
    private static final String KEY_FRAGMENT_ID = "fragment_id_key";
    public static final int FRAGMENT_NEARBY = 1;
    public static final int FRAGMENT_FAVORITES = 2;
    public static final int FRAGMENT_ALL = 3;

    private ArrayList<VehicleWithFavorite> vehicles;
    private VehiclesListAdapter vehiclesListAdapter;
    private String emptyViewContent;
    private TextView emptyView;

    /* newInstance constructor for creating fragment with arguments */
    public static VehiclesListFragment newInstance(int fragmentId, String emptyListText) {
        VehiclesListFragment vehiclesListFragment = new VehiclesListFragment();
        Bundle args = new Bundle();
        args.putString(KEY_EMPTY_LIST_TEXT, emptyListText);
        args.putInt(KEY_FRAGMENT_ID, fragmentId);
        vehiclesListFragment.setArguments(args);
        return vehiclesListFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(savedInstanceState != null) {
            emptyViewContent = savedInstanceState.getString(KEY_EMPTY_LIST_TEXT);
        } else if (args != null) {
            emptyViewContent = args.getString(KEY_EMPTY_LIST_TEXT);
        }
        VehiclesViewModel model = new ViewModelProvider(requireActivity()).get(VehiclesViewModel.class);
        if (args != null) {
            switch (args.getInt(KEY_FRAGMENT_ID)) {
                case 1 -> vehicles = new ArrayList<>();
                case 2 -> {
                    vehicles = new ArrayList<>();
                    model.getFavoriteVehicles().observe(this, list -> {
                        if (list != null) {
                            updateVehiclesList(new ArrayList<>(list));
                        }
                    });
                }
                case 3 -> {
                    vehicles = new ArrayList<>();
                    model.getVehicles().observe(this, list -> {
                        if (list != null) {
                            updateVehiclesList(new ArrayList<>(list));
                        }
                    });
                }
            }
        }
        vehiclesListAdapter = new VehiclesListAdapter(getActivity(),
                vehicles);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vehicles_list, container, false);
        ListView listView = view.findViewById(R.id.vehiclesListView);
        listView.setAdapter(vehiclesListAdapter);
        emptyView = view.findViewById(R.id.emptyList);
        emptyView.setText(emptyViewContent);
        listView.setEmptyView(view.findViewById(R.id.emptyList));
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Intent intent = new Intent(getActivity(), MapActivity.class);
            intent.putExtra(KEY_VEHICLE, vehicles.get(position).getVehicle());
            startActivity(intent);
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view1, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view1, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (getActivity() != null) {
                    SwipeRefreshLayout refreshLayout = getActivity().findViewById(R.id.swipe_container);
                    if (refreshLayout != null) {
                        refreshLayout.setEnabled(firstVisibleItem == 0);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_EMPTY_LIST_TEXT, emptyView.getText().toString());
    }


    public void updateVehiclesList(ArrayList<VehicleWithFavorite> vehicles) {
        this.vehicles = vehicles;
        if (vehiclesListAdapter != null) {
            vehiclesListAdapter.clear();
            vehiclesListAdapter.addAll(vehicles);
            vehiclesListAdapter.notifyDataSetChanged();
        }
    }

    public void setEmptyView(int id) {
        if(emptyView != null) {
            emptyView.setText(id);
        }
   }

}
