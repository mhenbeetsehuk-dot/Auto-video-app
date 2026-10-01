import React,{useState} from "react";
import {View,Text,Pressable,StyleSheet,Alert,ActivityIndicator,ScrollView} from "react-native";
import * as ImagePicker from "expo-image-picker";
import {File} from "expo-file-system";
import {fetch as expoFetch} from "expo/fetch";

const API=process.env.EXPO_PUBLIC_API_URL||"http://localhost:8000";

export default function App(){
 const [video,setVideo]=useState(null),[busy,setBusy]=useState(false),[count,setCount]=useState(6),[length,setLength]=useState(30),[vertical,setVertical]=useState(true);
 async function pickVideo(){
   const permission=await ImagePicker.requestMediaLibraryPermissionsAsync();
   if(!permission.granted){Alert.alert("Permission required","Allow GameClip Publisher to access your videos in Settings.");return;}
   const r=await ImagePicker.launchImageLibraryAsync({mediaTypes:["videos"],allowsEditing:false,quality:1,videoExportPreset:"Passthrough"});
   if(!r.canceled)setVideo(r.assets[0]);
 }
 async function createClips(){
   if(!video){Alert.alert("Choose a video first");return}
   setBusy(true);
   try{
     const file=new File(video.uri);
     if(!file.exists)throw new Error("The selected video cannot be read. Please grant Photos/Videos permission and try again.");
     const f=new FormData();
     f.append("video",file);
     f.append("clip_count",String(count));
     f.append("clip_duration",String(length));
     f.append("vertical",String(vertical));
     const r=await expoFetch(API+"/api/create-clips",{method:"POST",body:f});
     const j=await r.json();
     if(!r.ok)throw new Error(j.detail||"Processing failed");
     Alert.alert("Done",`${(j.clips||[]).length} clips created and queued.`);
   }catch(e){Alert.alert("Could not create clips",String(e.message||e))}
   finally{setBusy(false)}
 }
 return <View style={s.bg}><ScrollView contentContainerStyle={s.body}>
   <Text style={s.title}>GAMECLIP PUBLISHER</Text>
   <Text style={s.sub}>Create • Queue • Publish</Text>
   <Text style={s.h}>Create clips</Text>
   <Pressable style={s.pick} onPress={pickVideo}><Text style={s.white}>{video?.fileName||video?.fileName||"📁 Choose gameplay video"}</Text></Pressable>
   <Text style={s.label}>NUMBER OF CLIPS</Text><View style={s.row}>{[3,6,10,15].map(n=><Pressable key={n} onPress={()=>setCount(n)} style={[s.chip,count===n&&s.active]}><Text style={s.white}>{n}</Text></Pressable>)}</View>
   <Text style={s.label}>CLIP LENGTH</Text><View style={s.row}>{[15,30,45,60].map(n=><Pressable key={n} onPress={()=>setLength(n)} style={[s.chip,length===n&&s.active]}><Text style={s.white}>{n}s</Text></Pressable>)}</View>
   <Pressable style={s.option} onPress={()=>setVertical(!vertical)}><Text style={s.white}>{vertical?"📱 9:16 Vertical":"🖥 Landscape"}</Text></Pressable>
   <Text style={s.note}>High-action sections are processed on the backend; music and scheduling stay server-side.</Text>
   {busy?<ActivityIndicator color="#fff" style={{marginTop:28}}/>:<Pressable style={s.primary} onPress={createClips}><Text style={s.white}>🔥 CREATE & QUEUE</Text></Pressable>}
 </ScrollView></View>
}
const s=StyleSheet.create({bg:{flex:1,backgroundColor:"#0b0f14"},body:{padding:22,paddingTop:70,paddingBottom:50},title:{color:"#fff",fontSize:27,fontWeight:"900"},sub:{color:"#8c97a5",marginTop:6},h:{color:"#fff",fontSize:28,fontWeight:"800",marginTop:28,marginBottom:18},pick:{backgroundColor:"#151b23",borderRadius:14,padding:20},label:{color:"#8792a0",fontSize:12,fontWeight:"700",marginTop:20,marginBottom:8},row:{flexDirection:"row",gap:8,flexWrap:"wrap"},chip:{backgroundColor:"#171e27",padding:12,borderRadius:20},active:{backgroundColor:"#2f81f7"},option:{backgroundColor:"#171e27",padding:15,borderRadius:12,marginTop:20},note:{color:"#7e8996",lineHeight:20,marginTop:20},primary:{backgroundColor:"#2f81f7",padding:17,borderRadius:13,alignItems:"center",marginTop:25},white:{color:"#fff",fontWeight:"700"}});