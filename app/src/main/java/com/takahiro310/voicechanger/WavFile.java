package com.takahiro310.voicechanger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WavFile {

    private byte[] RIFF = {'R','I','F','F'}; //wavファイルリフチャンクに書き込むチャンクID用
    private int fileSize = 36;
    private byte[] WAVE = {'W','A','V','E'}; //WAV形式でRIFFフォーマットを使用する
    private byte[] fmt = {'f','m','t',' '}; //fmtチャンク　スペースも必要
    private int fmtSize = 16; //fmtチャンクのバイト数
    private byte[] fmtID = {1, 0}; // フォーマットID リニアPCMの場合01 00 2byte
    private short chCount = 1; //チャネルカウント モノラルなので1 ステレオなら2にする
    private int bytePerSec = 44100 * (fmtSize / 8) * chCount; //データ速度
    private short blockSize = (short) ((fmtSize / 8) * chCount); //ブロックサイズ (Byte/サンプリングレート * チャンネル数)
    private short bitPerSample = 16; //サンプルあたりのビット数 WAVでは8bitか16ビットが選べる
    private byte[] data = {'d', 'a', 't', 'a'}; //dataチャンク
    private int dataSize = 0; //波形データのバイト数


    public void writeHeader(String fileName) {

        File file = new File(fileName);

        try (RandomAccessFile raf = new RandomAccessFile(file,"rw");) {

            raf.seek(0);
            raf.write(RIFF);
            raf.write(littleEndianInteger(fileSize + (int)file.length()));
            raf.write(WAVE);
            raf.write(fmt);
            raf.write(littleEndianInteger(fmtSize));
            raf.write(fmtID);
            raf.write(littleEndianShort(chCount));
            raf.write(littleEndianInteger(44100)); //サンプリング周波数
            raf.write(littleEndianInteger(bytePerSec));
            raf.write(littleEndianShort(blockSize));
            raf.write(littleEndianShort(bitPerSample));
            raf.write(data);
            raf.write(littleEndianInteger((int)file.length()));
        } catch (IOException e) {
            return;
        }
    }

    private byte[] littleEndianInteger(int i){
        byte[] buffer = new byte[4];
        buffer[0] = (byte) i;
        buffer[1] = (byte) (i >> 8);
        buffer[2] = (byte) (i >> 16);
        buffer[3] = (byte) (i >> 24);
        return buffer;
    }

    private byte[] littleEndianShort(short s){
        byte[] buffer = new byte[2];
        buffer[0] = (byte) s;
        buffer[1] = (byte) (s >> 8);
        return buffer;
    }
}
