import React from 'react';
import { Text } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { colors } from './src/theme';
import QuestsScreen from './screens/QuestsScreen';
import QuestDetailScreen from './screens/QuestDetailScreen';
import SubmitScreen from './screens/SubmitScreen';
import DashboardScreen from './screens/DashboardScreen';
import ProfileScreen from './screens/ProfileScreen';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

const navTheme = {
  ...DefaultTheme,
  dark: true,
  colors: {
    ...DefaultTheme.colors,
    background: colors.bg,
    card: colors.bgElevated,
    text: colors.text,
    border: colors.cardBorder,
    primary: colors.teal,
  },
};

const headerStyle = {
  headerStyle: { backgroundColor: colors.bgElevated },
  headerTintColor: colors.text,
  headerTitleStyle: { fontWeight: '700' },
};

function QuestsStack() {
  return (
    <Stack.Navigator screenOptions={headerStyle}>
      <Stack.Screen name="QuestsList" component={QuestsScreen} options={{ headerShown: false }} />
      <Stack.Screen
        name="QuestDetail"
        component={QuestDetailScreen}
        options={({ route }) => ({ title: route.params?.title || 'Quest', headerBackTitle: 'Back' })}
      />
      <Stack.Screen
        name="Submit"
        component={SubmitScreen}
        options={{ title: 'Complete Quest', headerBackTitle: 'Back' }}
      />
    </Stack.Navigator>
  );
}

function tabIcon(emoji) {
  return ({ focused }) => (
    <Text style={{ fontSize: 20, opacity: focused ? 1 : 0.45 }}>{emoji}</Text>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <NavigationContainer theme={navTheme}>
        <Tab.Navigator
          screenOptions={{
            headerShown: false,
            tabBarStyle: { backgroundColor: colors.bgElevated, borderTopColor: colors.cardBorder },
            tabBarActiveTintColor: colors.teal,
            tabBarInactiveTintColor: colors.textFaint,
          }}
        >
          <Tab.Screen name="Quests" component={QuestsStack} options={{ tabBarIcon: tabIcon('🧭') }} />
          <Tab.Screen name="Dashboard" component={DashboardScreen} options={{ tabBarIcon: tabIcon('📊') }} />
          <Tab.Screen name="Profile" component={ProfileScreen} options={{ tabBarIcon: tabIcon('👤') }} />
        </Tab.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
