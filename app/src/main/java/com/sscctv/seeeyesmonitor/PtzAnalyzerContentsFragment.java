package com.sscctv.seeeyesmonitor;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PtzAnalyzerContentsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PtzAnalyzerContentsFragment extends PtzContentsFragment {
    private class PtzPacket {
        public final int address;
        public final String command;
        public final String packet;

        public PtzPacket(char address, String command, String packet) {
            this.address = address;
            this.command = command;
            this.packet = packet;
        }
    }

    private ArrayList<PtzPacket> mPackets;

    private class ViewHolder {
        TextView address;
        TextView command;
        TextView packet;
    }

    private ArrayAdapter<PtzPacket> mAdapter;
    private int mMaxListItems;

    public PtzAnalyzerContentsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PtzAnalyzerContentsFragment.
     */
    public static PtzAnalyzerContentsFragment newInstance() {
        PtzAnalyzerContentsFragment fragment = new PtzAnalyzerContentsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ptz_analyzer_contents, container, false);

        mPackets = new ArrayList<>();

        mAdapter = new ArrayAdapter<PtzPacket>(getContext(), R.layout.ptz_analyzer_item, mPackets) {
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder viewHolder;

                if (convertView == null) {
                    viewHolder = new ViewHolder();

                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.ptz_analyzer_item, parent, false);

                    viewHolder.address = (TextView) convertView.findViewById(R.id.ptz_packet_ch);
                    viewHolder.command = (TextView) convertView.findViewById(R.id.ptz_packet_command);
                    viewHolder.packet = (TextView) convertView.findViewById(R.id.ptz_packet_bytes);

                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                PtzPacket packet = getItem(position);

                viewHolder.address.setText(String.format(Locale.KOREAN, " %03d", packet.address));
                viewHolder.command.setText(String.format(Locale.KOREAN, "%s ", packet.command));
                viewHolder.packet.setText(packet.packet);

                return convertView;
            }
        };

        ListView listView = (ListView) view.findViewById(R.id.ptz_analyzer_contents);

        listView.setAdapter(mAdapter);

        mMaxListItems = view.getResources().getInteger(R.integer.ptz_analyzer_lines);

        return view;
    }

    public void addPacket(char ch, String command, String packet) {
        PtzPacket ptzPacket = new PtzPacket(ch, command, packet);

        mPackets.add(ptzPacket);
        while (mPackets.size() > mMaxListItems) {
            mPackets.remove(0);
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void clear() {
        mPackets.clear();
        mAdapter.notifyDataSetChanged();
    }
}